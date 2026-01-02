package com.hotel.notification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "booking_id", nullable = false)
    private Long bookingId;
    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;
    @Column(name = "notification_type", nullable = false)
    private String notificationType;
    @Column(nullable = false)
    private String subject;
    @Lob
    @Column(nullable = false)
    private String content;
    @Column(nullable = false)
    private String status;
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    @Column(name = "error_message")
    private String errorMessage;
}
