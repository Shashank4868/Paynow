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

    public void send(String recipient, String subject, FraudDecision message) {
        SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(recipient);
        email.setSubject(subject);
        email.setText(message.toString());

        mailSender.send(email);
    }
}