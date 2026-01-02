package com.hotel.notification.service;

import com.hotel.notification.entity.Notification;
import com.hotel.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    @Transactional
    public void sendNotification(Long bookingId, String recipientEmail, String notificationType,
                                 String subject, String templateName, Map<String, Object> variables) {
        Notification notification = Notification.builder()
                .bookingId(bookingId)
                .recipientEmail(recipientEmail)
                .notificationType(notificationType)
                .subject(subject)
                .content("Email content: " + templateName)
                .status("PENDING")
                .build();

        try {
            emailService.sendEmail(recipientEmail, subject, templateName, variables);

            notification.setStatus("SENT");
            notification.setSentAt(LocalDateTime.now());

            log.info("Notification sent successfully - Type: {}, BookingId: {}, Email: {}",
                    notificationType, bookingId, recipientEmail);

        } catch (Exception e) {
            notification.setStatus("FAILED");
            notification.setErrorMessage(e.getMessage());

            log.error("Failed to send notification - Type: {}, BookingId: {}, Email: {}, Error: {}",
                    notificationType, bookingId, recipientEmail, e.getMessage());
        }

        notificationRepository.save(notification);
    }
}