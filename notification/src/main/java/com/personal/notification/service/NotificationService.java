package com.personal.notification.service;

import com.personal.notification.dto.NotificationEvent;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final EmailService emailService;

    public NotificationService(EmailService emailService) {
        this.emailService = emailService;
    }

    public void process(NotificationEvent event) {
        try {
        emailService.send(
                event.recipient(),
                event.subject(),
                event.message()
        );
        }  catch (Exception e) {
            e.printStackTrace();
        }
    }
}