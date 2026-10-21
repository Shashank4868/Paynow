
Each Java service is an independent Maven project and includes a Maven wrapper (`mvnw` on macOS/Linux, `mvnw.cmd` on Windows).

## Local setup

### 1. Start Kafka

From the repository root:

```sh
docker compose -f kafka-local/docker-compose.yml up -d
```

The broker listens on `localhost:9092` for applications running on the host. To stop it, run `docker compose -f kafka-local/docker-compose.yml down`.

### 2. Configure PostgreSQL

Create a PostgreSQL database named `paynow` (the payment and auth services both use it). Both services read the database password from `DB_PASSWORD`; `DB_URL` (default `jdbc:postgresql://localhost:5432/paynow`) and `DB_USERNAME` (default `postgres`) can also be overridden.

Secrets are never kept in `application.properties`. For local runs each service activates its `local` profile, which loads a gitignored `src/main/resources/application-local.properties`. Copy the matching `application-local.properties.example` and fill in real values:

```sh
cp paymentService/src/main/resources/application-local.properties.example paymentService/src/main/resources/application-local.properties
cp auth-service/src/main/resources/application-local.properties.example auth-service/src/main/resources/application-local.properties
```

Environment variables with the same names override these files.

The payment service also reads an optional `paymentService/.env` (see `paymentService/.env.example`) from its working directory, which is handy when launching it from an IDE.

### 3. Start Keycloak on port 8180

The gateway owns port 8080, so Keycloak runs on **8180**. All services expect the issuer `http://localhost:8180/realms/paynow` (override with `KEYCLOAK_ISSUER_URI`).

```sh
# Keycloak distribution
bin/kc.sh start-dev --http-port=8180        # Windows: bin\kc.bat start-dev --http-port=8180
# or Docker
docker run -p 8180:8080 -e KC_BOOTSTRAP_ADMIN_USERNAME=admin -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin quay.io/keycloak/keycloak start-dev
```

Create a realm `paynow` and a client `paynow`; put the client secret in auth-service's `KEYCLOAK_CLIENT_SECRET`. Users need an email address, because the payment service reads the sender's email from the token's `email` claim.

### 4. Configure notification email (optional)

Set `MAIL_USERNAME` and `MAIL_PASSWORD` in the environment used to launch `notification`. The service uses Gmail SMTP (`smtp.gmail.com:587`) with authentication and STARTTLS. Use an SMTP app password where required. Do not commit real credentials to the repository.

### 5. Run the services

Start each command in its own terminal from the repository root:

```sh
# Windows PowerShell
.\paymentService\mvnw.cmd -f paymentService\pom.xml spring-boot:run
.\fraud-detection\mvnw.cmd -f fraud-detection\pom.xml spring-boot:run
.\notification\mvnw.cmd -f notification\pom.xml spring-boot:run
.\auth-service\mvnw.cmd -f auth-service\pom.xml spring-boot:run
.\api-gateway\mvnw.cmd -f api-gateway\pom.xml spring-boot:run
```

On macOS/Linux, replace `mvnw.cmd` with `mvnw` and use `/` in paths. Start Kafka, PostgreSQL and Keycloak first.

| Component | Port |
| --- | --- |
| api-gateway | 8080 |
| paymentService | 8081 |
| fraud-detection | 8082 |
| auth-service | 8083 |
| notification | 8089 |
| Keycloak | 8180 |
| Kafka | 9092 |

## API

Call every endpoint through the gateway (`http://localhost:8080`); clients don't need the service ports. Every request needs a Keycloak access token in `Authorization: Bearer <token>`, which the gateway checks and then passes on unchanged. The payment and auth services validate the token themselves too, so calling them directly on 8081 or 8083 without one returns `401`.

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/users/me` | Create or update the signed-in user's profile from the token and return it (`profileId`, `username`, `email`) |
| `POST` | `/payments/create-payment` | Create a payment from the signed-in user; its `payment.created` event is sent once the payment is saved |
| `GET` | `/payments/{id}` | Look up a payment you sent or received |

Getting a token for testing (requires "Direct access grants" enabled on the client):

```sh
curl -s -X POST http://localhost:8180/realms/paynow/protocol/openid-connect/token \
  -d grant_type=password -d client_id=paynow -d client_secret=<secret> \
  -d username=<user> -d password=<password>
```

Example request:

```http
POST http://localhost:8080/payments/create-payment
Content-Type: application/json
Authorization: Bearer <access token>
```

```json
{
  "receiverId": "account-200",
  "receiverEmail": "receiver@example.com",
  "amount": 125.50,
  "currency": "INR",
  "idempotencyKey": "order-2026-0001",
  "reference": "Order 0001"
}
```

There is no `senderId` or `senderEmail` field: the sender is always the signed-in user (the token's `sub` and `email` claims). `receiverId`, `receiverEmail`, `amount`, `currency` and `idempotencyKey` are required; `amount` must be at least `0.01`, and `currency` must be a three-letter uppercase code. Paying yourself returns `400`, and reusing an idempotency key returns `409`.

A successful create returns `201` with a `Location: /payments/{id}` header and the payment (`id`, `status`, `idempotencyKey`, timestamps, and so on). `GET /payments/{id}` returns the same shape, but only to the payment's sender or receiver; anyone else gets `404`.

### Payment status lifecycle

| From | Allowed next statuses |
| --- | --- |
| `PENDING` | `COMPLETED` (fraud approved), `FLAGGED` (fraud flagged), `FAILED`, `CANCELLED`, `PROCESSING` |
| `PROCESSING` | `COMPLETED`, `FAILED` |
| `FLAGGED` | `FAILED`, `CANCELLED` (a flagged payment is never auto-approved) |
| `COMPLETED`, `FAILED`, `CANCELLED` | none (terminal) |

Any other move is rejected with an `InvalidStateTransitionException`. A repeated decision for the status a payment already has (for example a redelivered Kafka event) is ignored. `PROCESSING` is reserved for a future ledger step.

## Event flow

1. `paymentService` saves the new payment as `PENDING`. Once that transaction commits, it sends `payment.created`. A payment that is rolled back is never announced.
2. `fraud-detection` evaluates it and sends only an `APPROVED` or `FLAGGED` decision to `payment.decision`. It does not send any notification.
3. `paymentService` applies the decision (`COMPLETED` or `FLAGGED`). Once the status change commits, it sends exactly one message to the notification topic: `{recipient, sender, subject, message}`, where `message` is `APPROVED` or `FLAGGED`. A repeated decision that leaves the status unchanged sends nothing.
4. `paymentService` picks who is emailed: the receiver for `APPROVED`, and only the sender for `FLAGGED` (the payment is under review; the receiver is not told). `notification` just sends to the event's `recipient`.

**Release `fraud-detection` and `paymentService` together.** Notifications moved from fraud-detection to paymentService. A new fraud-detection with an old paymentService sends no emails. An old fraud-detection with a new paymentService sends every email twice.

## Configuration notes

- Payment event topic and consumer group defaults are `payment.created`, `payment.decision`, and `process-payment`. They can be overridden with `PAYMENT_CREATED_TOPIC`, `PAYMENT_DECISION_TOPIC`, and `KAFKA_CONSUMER_GROUP_ID`.
- `NOTIFICATION_KAFKA_TOPIC` (default `notification-kafka-topic`) is read by both `paymentService` (producer) and `notification` (consumer), so set it to the same value in both. Notification also reads `CONSUMER_GROUP` (default `notification-service`).
- fraud-detection reads the same `PAYMENT_CREATED_TOPIC` and `PAYMENT_DECISION_TOPIC` as the payment service, so set them once for both. Its consumer group defaults to `fraud-detection` (override with `FRAUD_CONSUMER_GROUP_ID`), and its broker to `localhost:9092` (`KAFKA_BOOTSTRAP_SERVERS`).
- The gateway validates Keycloak JWTs and routes `/payments/**` to the payment service (`PAYMENT_SERVICE_URL`, default `http://localhost:8081`) and `/users/**` to the auth service (`AUTH_SERVICE_URL`, default `http://localhost:8083`).

## Project layout

```text
.
├── api-gateway/       # Spring Boot gateway/security application
├── auth-service/      # Keycloak-backed user profiles (/users/me)
├── fraud-detection/   # Kafka fraud evaluation and decision publishing
├── kafka-local/       # Local Kafka Compose definition
├── notification/     # Kafka consumer and email delivery
├── paymentService/    # Payment API, persistence, and Kafka integration
└── README.md
```
