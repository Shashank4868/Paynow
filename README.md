
Each Java service is an independent Maven project and includes a Maven wrapper (`mvnw` on macOS/Linux, `mvnw.cmd` on Windows).

## Local setup

### 1. Start Kafka

From the repository root:

```sh
docker compose -f kafka-local/docker-compose.yml up -d
```

The broker listens on `localhost:9092` for applications running on the host. To stop it, run `docker compose -f kafka-local/docker-compose.yml down`.

### 2. Configure PostgreSQL

Create a PostgreSQL database named `payments`. The payment service currently defaults to `jdbc:postgresql://localhost:5432/payments` with username `postgres` and password `1234`; set `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` to match your local database. Liquibase applies the schema from `paymentService/src/main/resources/db/changelog/db.changelog-master.yaml`.

### 3. Configure notification email (optional)

Set `MAIL_USERNAME` and `MAIL_PASSWORD` in the environment used to launch `notification`. The service uses Gmail SMTP (`smtp.gmail.com:587`) with authentication and STARTTLS. Use an SMTP app password where required. Do not commit real credentials to the repository.

### 4. Run the services

Start each command in its own terminal from the repository root:

```sh
# Windows PowerShell
.\paymentService\mvnw.cmd -f paymentService\pom.xml spring-boot:run
.\fraud-detection\mvnw.cmd -f fraud-detection\pom.xml spring-boot:run
.\notification\mvnw.cmd -f notification\pom.xml spring-boot:run
.\api-gateway\mvnw.cmd -f api-gateway\pom.xml spring-boot:run
```

On macOS/Linux, replace `mvnw.cmd` with `mvnw` and use `/` in paths. Start Kafka and PostgreSQL before the payment service. The fraud and notification services have local profile configuration for `localhost:9092`; the payment service activates its `local` profile by default. The gateway currently has no route configuration, so use the payment service API directly.

## Payment API

The payment service exposes:

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/payments/create-payment` | Create a payment and publish its created event |
| `GET` | `/payments/{id}` | Look up a payment by its UUID |

Example request:

```http
POST http://localhost:8080/payments/create-payment
Content-Type: application/json
```

```json
{
  "senderId": "account-100",
  "receiverId": "account-200",
  "amount": 125.50,
  "currency": "INR",
  "idempotencyKey": "order-2026-0001",
  "reference": "Order 0001"
}
```

`senderId`, `receiverId`, and `idempotencyKey` are required. `amount` must be at least `0.01`, and `currency` must be a three-letter uppercase code. Reusing an idempotency key returns a conflict. A successful create returns HTTP `201` with a payment representation including its UUID and current status; use that UUID with `GET /payments/{id}` to retrieve it later.

## Configuration notes

- Payment event topic and consumer group defaults are `payment.created`, `payment.decision`, and `process-payment`. They can be overridden with `PAYMENT_CREATED_TOPIC`, `PAYMENT_DECISION_TOPIC`, and `KAFKA_CONSUMER_GROUP_ID`.
- Notification uses `NOTIFICATION_KAFKA_TOPIC` (default `notification-kafka-topic`) and `CONSUMER_GROUP` (default `notification-service`).
- The fraud service currently uses its topic names directly in its listener and publisher.
- The gateway's current `application.properties` only sets the application name. Its dependencies include OAuth2 resource-server security and Redis-backed sessions, but authentication settings and gateway routes still need configuration.

## Project layout

```text
.
├── api-gateway/       # Spring Boot gateway/security application
├── fraud-detection/   # Kafka fraud evaluation and decision publishing
├── kafka-local/       # Local Kafka Compose definition
├── notification/     # Kafka consumer and email delivery
├── paymentService/    # Payment API, persistence, and Kafka integration
└── README.md
```
