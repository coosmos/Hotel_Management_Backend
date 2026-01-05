package com.hotel.booking.repository;

import com.hotel.booking.entity.Booking;
import com.hotel.booking.enums.BookingStatus;
import com.hotel.booking.enums.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class BookingRepositoryTest {

    @Autowired
    private BookingRepository bookingRepository;

    private Booking createBooking(
            Long roomId,
            BookingStatus status,
            LocalDate checkIn,
            LocalDate checkOut
    ) {
        return Booking.builder()
                .userId(1L)
                .hotelId(1L)
                .roomId(roomId)
                .hotelName("Test Hotel")
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .status(status)
                .paymentStatus(PaymentStatus.PAID)
                .guestName("Test User")
                .guestEmail("test@mail.com")
                .guestPhone("9999999999")
                .numberOfGuests(2)
                .totalAmount(1000f)
                .build();
    }

    @Test
    void findBookedRoomIds_shouldReturnOnlyOverlappingActiveRooms() {
        bookingRepository.save(
                createBooking(101L, BookingStatus.CONFIRMED,
                        LocalDate.of(2025, 1, 10),
                        LocalDate.of(2025, 1, 15))
        );

        bookingRepository.save(
                createBooking(102L, BookingStatus.CANCELLED,
                        LocalDate.of(2025, 1, 10),
                        LocalDate.of(2025, 1, 15))
        );

        List<Long> bookedRooms = bookingRepository.findBookedRoomIds(
                1L,
                LocalDate.of(2025, 1, 12),
                LocalDate.of(2025, 1, 14)
        );

        assertThat(bookedRooms).containsExactly(101L);
    }

    @Test
    void isRoomBooked_shouldReturnTrueForOverlappingBooking() {
        bookingRepository.save(
                createBooking(201L, BookingStatus.CONFIRMED,
                        LocalDate.of(2025, 2, 1),
                        LocalDate.of(2025, 2, 5))
        );

        boolean booked = bookingRepository.isRoomBooked(
                201L,
                LocalDate.of(2025, 2, 3),
                LocalDate.of(2025, 2, 4)
        );

        assertThat(booked).isTrue();
    }

    @Test
    void isRoomBooked_shouldReturnFalseForNonOverlappingBooking() {
        bookingRepository.save(
                createBooking(202L, BookingStatus.CONFIRMED,
                        LocalDate.of(2025, 2, 1),
                        LocalDate.of(2025, 2, 5))
        );

        boolean booked = bookingRepository.isRoomBooked(
                202L,
                LocalDate.of(2025, 2, 6),
                LocalDate.of(2025, 2, 7)
        );

        assertThat(booked).isFalse();
    }

    @Test
    void findActiveBookingsByUserId_shouldReturnConfirmedAndCheckedInOnly() {
        bookingRepository.save(createBooking(301L, BookingStatus.CONFIRMED,
                LocalDate.now(), LocalDate.now().plusDays(1)));

        bookingRepository.save(createBooking(302L, BookingStatus.CHECKED_IN,
                LocalDate.now(), LocalDate.now().plusDays(1)));

        bookingRepository.save(createBooking(303L, BookingStatus.CANCELLED,
                LocalDate.now(), LocalDate.now().plusDays(1)));

        List<Booking> bookings = bookingRepository.findActiveBookingsByUserId(1L);

        assertThat(bookings)
                .hasSize(2)
                .allMatch(b -> b.getStatus() != BookingStatus.CANCELLED);
    }

    @Test
    void countActiveBookings_shouldCountConfirmedAndCheckedIn() {
        bookingRepository.save(createBooking(401L, BookingStatus.CONFIRMED,
                LocalDate.now(), LocalDate.now().plusDays(1)));

        bookingRepository.save(createBooking(402L, BookingStatus.CHECKED_IN,
                LocalDate.now(), LocalDate.now().plusDays(1)));

        bookingRepository.save(createBooking(403L, BookingStatus.CHECKED_OUT,
                LocalDate.now(), LocalDate.now().plusDays(1)));

        Long count = bookingRepository.countActiveBookings(1L);

        assertThat(count).isEqualTo(2);
    }

    @Test
    void findOverdueConfirmedBookings_shouldReturnPastConfirmedBookings() {
        bookingRepository.save(createBooking(501L, BookingStatus.CONFIRMED,
                LocalDate.now().minusDays(3),
                LocalDate.now().minusDays(1)));
        List<Booking> overdue =
                bookingRepository.findOverdueConfirmedBookings(LocalDate.now());
        assertThat(overdue).hasSize(1);
    }
}
