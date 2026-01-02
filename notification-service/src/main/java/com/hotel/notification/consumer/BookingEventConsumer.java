package com.hotel.notification.consumer;

import com.hotel.notification.model.BookingCreatedEvent;
import com.hotel.notification.model.CheckInReminderEvent;
import com.hotel.notification.model.GuestCheckedInEvent;
import com.hotel.notification.model.GuestCheckedOutEvent;
import com.hotel.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEventConsumer {

    private final NotificationService notificationService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM dd, yyyy");

    @KafkaListener(topics = "${kafka.topics.booking-created}", containerFactory = "bookingCreatedKafkaListenerContainerFactory")
    public void consumeBookingCreated(BookingCreatedEvent event) {

        log.info("Received booking-created event for BookingId: {}", event.getBookingId());

        Map<String, Object> variables = new HashMap<>();
        variables.put("guestName", event.getGuestName());
        variables.put("bookingId", event.getBookingId());
        variables.put("checkInDate", event.getCheckInDate().format(DATE_FORMATTER));
        variables.put("checkOutDate", event.getCheckOutDate().format(DATE_FORMATTER));
        variables.put("numberOfGuests", event.getNumberOfGuests());
        variables.put("totalAmount", String.format("%.2f", event.getTotalAmount()));
        variables.put("specialRequests", event.getSpecialRequests() != null ? event.getSpecialRequests() : "None");

        String subject = "Booking Confirmation - Your reservation is confirmed!";

        notificationService.sendNotification(
                event.getBookingId(),
                event.getGuestEmail(),
                "BOOKING_CONFIRMATION",
                subject,
                "booking-confirmation",
                variables
        );
    }

    @KafkaListener(topics = "${kafka.topics.checkin-reminder}", containerFactory = "checkInReminderKafkaListenerContainerFactory")
    public void consumeCheckInReminder(CheckInReminderEvent event) {

        log.info("Received checkin-reminder event for BookingId: {}", event.getBookingId());

        Map<String, Object> variables = new HashMap<>();
        variables.put("guestName", event.getGuestName());
        variables.put("bookingId", event.getBookingId());
        variables.put("hotelName", event.getHotelName());
        variables.put("roomNumber", event.getRoomNumber());
        variables.put("checkInDate", event.getCheckInDate().format(DATE_FORMATTER));
        variables.put("checkOutDate", event.getCheckOutDate().format(DATE_FORMATTER));
        String subject = "Check-in Reminder - Tomorrow is your check-in day!";

        notificationService.sendNotification(
                event.getBookingId(),
                event.getGuestEmail(),
                "CHECKIN_REMINDER",
                subject,
                "checkin-reminder",
                variables
        );
    }

    @KafkaListener(topics = "${kafka.topics.guest-checked-in}", containerFactory = "guestCheckedInKafkaListenerContainerFactory")
    public void consumeGuestCheckedIn(GuestCheckedInEvent event) {

        log.info("Received guest-checked-in event for BookingId: {}", event.getBookingId());

        Map<String, Object> variables = new HashMap<>();
        variables.put("guestName", event.getGuestName());
        variables.put("bookingId", event.getBookingId());
        variables.put("checkInDate", event.getCheckInDate().format(DATE_FORMATTER));
        variables.put("checkOutDate", event.getCheckOutDate().format(DATE_FORMATTER));
        variables.put("checkedInTime", event.getCheckedInAt().format(DateTimeFormatter.ofPattern("hh:mm a")));

        String subject = "Welcome! You've successfully checked in";

        notificationService.sendNotification(
                event.getBookingId(),
                event.getGuestEmail(),
                "CHECKIN_SUCCESS",
                subject,
                "checkin-success",
                variables
        );
    }

    @KafkaListener(topics = "${kafka.topics.guest-checked-out}", containerFactory = "guestCheckedOutKafkaListenerContainerFactory")
    public void consumeGuestCheckedOut(GuestCheckedOutEvent event) {

        log.info("Received guest-checked-out event for BookingId: {}", event.getBookingId());

        Map<String, Object> variables = new HashMap<>();
        variables.put("guestName", event.getGuestName());
        variables.put("bookingId", event.getBookingId());
        variables.put("checkInDate", event.getCheckInDate().format(DATE_FORMATTER));
        variables.put("checkOutDate", event.getCheckOutDate().format(DATE_FORMATTER));
        variables.put("checkedOutTime", event.getCheckedOutAt().format(DateTimeFormatter.ofPattern("hh:mm a")));
        variables.put("rating", event.getRating() != null ? event.getRating() : "Not provided");
        variables.put("feedback", event.getFeedback() != null ? event.getFeedback() : "No feedback provided");

        String subject = "Thank you for staying with us!";

        notificationService.sendNotification(
                event.getBookingId(),
                event.getGuestEmail(),
                "CHECKOUT_THANKYOU",
                subject,
                "checkout-thankyou",
                variables
        );
    }
}