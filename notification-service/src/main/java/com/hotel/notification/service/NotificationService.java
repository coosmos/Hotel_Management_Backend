package com.hotel.notification.service;

import com.hotel.notification.entity.Notification;
import com.hotel.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    public void sendAndSave(
            Long bookingId,
            String recipientEmail,
            String notificationType,
            String subject,
            String template,
            Context context
    ) {
        Notification notification = Notification.builder()
                .bookingId(bookingId)
                .recipientEmail(recipientEmail)
                .notificationType(notificationType)
                .subject(subject)
                .status("PENDING")
                .build();

        notificationRepository.save(notification);
        boolean sent = emailService.sendEmail(
                recipientEmail,
                subject,
                template,
                context
        );
        if (sent) {
            notification.setStatus("SENT");
            notification.setSentAt(LocalDateTime.now());
        } else {
            notification.setStatus("FAILED");
            notification.setErrorMessage("Email sending failed");
        }
        notificationRepository.save(notification);
    }
}
