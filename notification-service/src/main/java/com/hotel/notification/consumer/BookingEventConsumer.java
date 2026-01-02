package com.hotel.notification.consumer;

import com.hotel.notification.model.*;
import com.hotel.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventConsumer {

    private final NotificationService notificationService;

    @Value("${kafka.topics.booking-created}")
    private String bookingCreatedTopic;

    @Value("${kafka.topics.checkin-reminder}")
    private String checkinReminderTopic;

    @Value("${kafka.topics.guest-checked-in}")
    private String guestCheckedInTopic;

    @Value("${kafka.topics.guest-checked-out}")
    private String guestCheckedOutTopic;

    @KafkaListener(topics = "${kafka.topics.booking-created}", groupId = "notification-group")
    public void handleBookingCreated(BookingCreatedEvent event) {
        log.info("Received booking-created event {}", event.getBookingId());

        Context context = new Context();
        context.setVariable("event", event);

        notificationService.sendAndSave(
                event.getBookingId(),
                event.getGuestEmail(),
                "BOOKING_CREATED",
                "Booking Confirmation",
                "booking-confirmation",
                context
        );
    }

    @KafkaListener(topics = "${kafka.topics.checkin-reminder}", groupId = "notification-group")
    public void handleCheckInReminder(CheckInReminderEvent event) {
        log.info("Received checkin-reminder event {}", event.getBookingId());

        Context context = new Context();
        context.setVariable("event", event);

        notificationService.sendAndSave(
                event.getBookingId(),
                event.getGuestEmail(),
                "CHECKIN_REMINDER",
                "Check-in Reminder",
                "checkin-reminder",
                context
        );
    }

    @KafkaListener(topics = "${kafka.topics.guest-checked-in}", groupId = "notification-group")
    public void handleGuestCheckedIn(GuestCheckedInEvent event) {
        log.info("Received guest-checked-in event {}", event.getBookingId());

        Context context = new Context();
        context.setVariable("event", event);

        notificationService.sendAndSave(
                event.getBookingId(),
                event.getGuestEmail(),
                "CHECKIN_SUCCESS",
                "Check-in Successful",
                "checkin-success",
                context
        );
    }

    @KafkaListener(topics = "${kafka.topics.guest-checked-out}", groupId = "notification-group")
    public void handleGuestCheckedOut(GuestCheckedOutEvent event) {
        log.info("Received guest-checked-out event {}", event.getBookingId());

        Context context = new Context();
        context.setVariable("event", event);

        notificationService.sendAndSave(
                event.getBookingId(),
                event.getGuestEmail(),
                "CHECKOUT_THANKYOU",
                "Thank You for Staying With Us",
                "checkout-thankyou",
                context
        );
    }
}
