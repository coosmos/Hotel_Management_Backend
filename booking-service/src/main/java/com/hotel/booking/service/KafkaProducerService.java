package com.hotel.booking.service;

import com.hotel.booking.event.BookingCreatedEvent;
import com.hotel.booking.event.CheckInReminderEvent;
import com.hotel.booking.event.GuestCheckedInEvent;
import com.hotel.booking.event.GuestCheckedOutEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    @Value("${kafka.topics.booking-created}")
    private String bookingCreatedTopic;
    @Value("${kafka.topics.guest-checked-in}")
    private String guestCheckedInTopic;
    @Value("${kafka.topics.guest-checked-out}")
    private String guestCheckedOutTopic;
    public void publishBookingCreated(BookingCreatedEvent event) {
        try {
            log.info("Publishing booking-created event for booking ID: {}", event.getBookingId());

            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(bookingCreatedTopic, event.getBookingId().toString(), event);
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish booking-created event: {}", ex.getMessage());
                } else {
                    log.info("Successfully published booking-created event to topic: {}",
                            bookingCreatedTopic);
                }
            });
        } catch (Exception e) {
            log.error("Error publishing booking-created event: {}", e.getMessage(), e);
        }
    }
    public void publishGuestCheckedIn(GuestCheckedInEvent event) {
        try {
            log.info("Publishing guest-checked-in event for booking ID: {}", event.getBookingId());

            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(guestCheckedInTopic, event.getBookingId().toString(), event);
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish guest-checked-in event: {}", ex.getMessage());
                } else {
                    log.info("Successfully published guest-checked-in event to topic: {}",
                            guestCheckedInTopic);
                }
            });
        } catch (Exception e) {
            log.error("Error publishing guest-checked-in event: {}", e.getMessage(), e);
        }
    }
    public void publishGuestCheckedOut(GuestCheckedOutEvent event) {
        try {
            log.info("Publishing guest-checked-out event for booking ID: {}", event.getBookingId());
            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(guestCheckedOutTopic, event.getBookingId().toString(), event);
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish guest-checked-out event: {}", ex.getMessage());
                } else {
                    log.info("Successfully published guest-checked-out event to topic: {}",
                            guestCheckedOutTopic);
                }
            });
        } catch (Exception e) {
            log.error("Error publishing guest-checked-out event: {}", e.getMessage(), e);
        }
    }
    @Value("${kafka.topics.checkin-reminder}")
    private String checkInReminderTopic;

    //publish checkin reminder
    public void publishCheckInReminder(CheckInReminderEvent event) {
        try {
            log.info("Publishing check-in reminder for booking ID: {}", event.getBookingId());

            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(checkInReminderTopic, event.getBookingId().toString(), event);

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish check-in reminder: {}", ex.getMessage());
                } else {
                    log.info("Successfully published check-in reminder to topic: {}",
                            checkInReminderTopic);
                }
            });
        } catch (Exception e) {
            log.error("Error publishing check-in reminder: {}", e.getMessage(), e);
        }
    }
}