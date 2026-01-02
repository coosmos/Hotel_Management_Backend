package com.hotel.booking.scheduler;

import com.hotel.booking.client.HotelServiceClient;
import com.hotel.booking.dto.external.RoomDto;
import com.hotel.booking.entity.Booking;
import com.hotel.booking.enums.BookingStatus;
import com.hotel.booking.event.CheckInReminderEvent;
import com.hotel.booking.repository.BookingRepository;
import com.hotel.booking.service.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

//scheduler for booking reminder
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingReminderScheduler {

    private final BookingRepository bookingRepository;
    private final HotelServiceClient hotelServiceClient;
    private final KafkaProducerService kafkaProducerService;

    //sends check in reminders -24 hours before checkin
    @Scheduled(cron = "0 0 9 * * *") // for testing , in production it will be sent 0 0 9 * * *
    public void sendCheckInReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        // Find all confirmed bookings with check-in tomorrow
        List<Booking> upcomingBookings = bookingRepository
                .findByCheckInDateAndStatus(tomorrow, BookingStatus.CONFIRMED);
        log.info("Found {} bookings with check-in tomorrow ({})",
                upcomingBookings.size(), tomorrow);
        upcomingBookings.forEach(booking -> {
            try {
                // Fetch room details
                RoomDto room = hotelServiceClient.getRoomById(booking.getRoomId());
                // Create reminder event
                CheckInReminderEvent event = CheckInReminderEvent.builder()
                        .bookingId(booking.getId())
                        .userId(booking.getUserId())
                        .guestName(booking.getGuestName())
                        .guestEmail(booking.getGuestEmail())
                        .guestPhone(booking.getGuestPhone())
                        .hotelId(booking.getHotelId())
                        .hotelName("Hotel Name") // Can fetch from hotel-service
                        .roomId(booking.getRoomId())
                        .roomNumber(room.getRoomNumber())
                        .checkInDate(booking.getCheckInDate())
                        .checkOutDate(booking.getCheckOutDate())
                        .build();
                kafkaProducerService.publishCheckInReminder(event);
                log.info("Sent check-in reminder for booking {}", booking.getId());
            } catch (Exception e) {
                log.error("Failed to send reminder for booking {}: {}",
                        booking.getId(), e.getMessage());
            }
        });
        log.info("Check-in reminder job completed");
    }
    //send checkout reminder
    @Scheduled(cron = "0 0 8 * * *")
    public void sendCheckOutReminders() {
        log.info("Starting check-out reminder job...");
        LocalDate today = LocalDate.now();
        // Find all checked-in bookings with check-out today
        List<Booking> checkingOutToday = bookingRepository
                .findByCheckOutDateAndStatus(today, BookingStatus.CHECKED_IN);
        log.info("Found {} bookings with check-out today", checkingOutToday.size());
        checkingOutToday.forEach(booking -> {
            // Send reminder to staff (not guest)
            log.info("Check-out today for booking {}, guest: {}",
                    booking.getId(), booking.getGuestName());
            // Can publish event for notification-service
        });
    }

}
