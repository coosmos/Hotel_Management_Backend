package com.hotel.booking.repository;

import com.hotel.booking.entity.Booking;
import com.hotel.booking.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Booking> findByHotelIdOrderByCreatedAtDesc(Long hotelId);
    List<Booking> findByStatus(BookingStatus status);
    List<Booking> findByHotelIdAndStatus(Long hotelId, BookingStatus status);

    @Query("""
        SELECT DISTINCT b.roomId FROM Booking b
        WHERE b.hotelId = :hotelId
        AND b.status NOT IN ('CANCELLED', 'CHECKED_OUT')
        AND (
            (b.checkInDate <= :checkOutDate AND b.checkOutDate >= :checkInDate)
        )
        """)
    List<Long> findBookedRoomIds(
            @Param("hotelId") Long hotelId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate
    );

    /**
     * Check if a specific room is booked for date range
     */
    @Query("""
        SELECT COUNT(b) > 0 FROM Booking b
        WHERE b.roomId = :roomId
        AND b.status NOT IN ('CANCELLED', 'CHECKED_OUT')
        AND (
            (b.checkInDate <= :checkOutDate AND b.checkOutDate >= :checkInDate)
        )
        """)
    boolean isRoomBooked(
            @Param("roomId") Long roomId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate
    );

    @Query("""
        SELECT b FROM Booking b
        WHERE b.hotelId = :hotelId
        AND b.status = 'CONFIRMED'
        AND b.checkInDate = :date
        ORDER BY b.checkInDate
        """)
    List<Booking> findUpcomingCheckIns(
            @Param("hotelId") Long hotelId,
            @Param("date") LocalDate date
    );
    //to find upcoming checkout for a hotel
    @Query("""
        SELECT b FROM Booking b
        WHERE b.hotelId = :hotelId
        AND b.status = 'CHECKED_IN'
        AND b.checkOutDate = :date
        ORDER BY b.checkOutDate
        """)
    List<Booking> findUpcomingCheckOuts(
            @Param("hotelId") Long hotelId,
            @Param("date") LocalDate date
    );

    // find active bookings for a user
    @Query("""
        SELECT b FROM Booking b
        WHERE b.userId = :userId
        AND b.status IN ('CONFIRMED', 'CHECKED_IN')
        ORDER BY b.checkInDate DESC
        """)
    List<Booking> findActiveBookingsByUserId(@Param("userId") Long userId);
    //find bookings by date range
    @Query("""
        SELECT b FROM Booking b
        WHERE b.hotelId = :hotelId
        AND (
            (b.checkInDate BETWEEN :startDate AND :endDate)
            OR (b.checkOutDate BETWEEN :startDate AND :endDate)
            OR (b.checkInDate <= :startDate AND b.checkOutDate >= :endDate)
        )
        ORDER BY b.checkInDate
        """)
    List<Booking> findByHotelIdAndDateRange(
            @Param("hotelId") Long hotelId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
    //count active bookings for a hotel
    @Query("""
        SELECT COUNT(b) FROM Booking b
        WHERE b.hotelId = :hotelId
        AND b.status IN ('CONFIRMED', 'CHECKED_IN')
        """)
    Long countActiveBookings(@Param("hotelId") Long hotelId);
}