package com.hotel.booking.repository;

import com.hotel.booking.entity.Booking;
import com.hotel.booking.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
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

    /**
     * Find conflicting bookings with PESSIMISTIC WRITE LOCK
     * This prevents race conditions during booking creation
     * How it works:
     * 1. Locks all matching rows (or locks the query range if no matches)
     * 2. Other transactions trying to query the same data will WAIT
     * 3. Lock is released when transaction commits
     *  roomId Room to check
     *  checkInDate Check-in date
     *  checkOutDate Check-out date
     * @return List of conflicting bookings (empty if available)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT b FROM Booking b
        WHERE b.roomId = :roomId
        AND b.status NOT IN ('CANCELLED', 'CHECKED_OUT')
        AND (
            (b.checkInDate <= :checkOutDate AND b.checkOutDate >= :checkInDate)
        )
        """)
    List<Booking> findConflictingBookingsWithLock(
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

    @Query("""
        SELECT b FROM Booking b
        WHERE b.userId = :userId
        AND b.status IN ('CONFIRMED', 'CHECKED_IN')
        ORDER BY b.checkInDate DESC
        """)
    List<Booking> findActiveBookingsByUserId(@Param("userId") Long userId);

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

    @Query("""
        SELECT COUNT(b) FROM Booking b
        WHERE b.hotelId = :hotelId
        AND b.status IN ('CONFIRMED', 'CHECKED_IN')
        """)
    Long countActiveBookings(@Param("hotelId") Long hotelId);

    List<Booking> findByCheckInDateAndStatus(LocalDate checkInDate, BookingStatus status);
    List<Booking> findByCheckOutDateAndStatus(LocalDate checkOutDate, BookingStatus status);

    @Query("""
        SELECT b FROM Booking b
        WHERE b.status = 'CONFIRMED'
        AND b.checkInDate < :date
        """)
    List<Booking> findOverdueConfirmedBookings(@Param("date") LocalDate date);

    @Query("""
        SELECT DISTINCT b.roomId FROM Booking b
        WHERE b.hotelId = :hotelId
        AND b.status NOT IN ('CANCELLED', 'CHECKED_OUT')
        AND (
            (b.checkInDate <= :checkOutDate AND b.checkOutDate >= :checkInDate)
        )
        """)
    List<Long> findBookedRoomIdsForDateRange(
            @Param("hotelId") Long hotelId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate
    );
}