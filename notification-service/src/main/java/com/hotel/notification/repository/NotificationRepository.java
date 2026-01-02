package com.hotel.notification.repository;

import com.hotel.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByBookingId(Long bookingId);
    List<Notification> findByRecipientEmail(String recipientEmail);
    List<Notification> findByNotificationType(String notificationType);
    List<Notification> findByStatus(String status);
}