package com.personal.notification.service;

import com.personal.notification.entity.FraudDecision;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void send(String recipient, String sender, String subject, FraudDecision message) {
        SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(recipient);
        email.setFrom(sender);
        email.setSubject(String.format("PayNow alert: %s - %s (from %s)", subject, message, sender));
        email.setText(String.format("Payment sender: %s%nReason: %s%nDecision: %s", sender, subject, message));

        mailSender.send(email);
    }
}