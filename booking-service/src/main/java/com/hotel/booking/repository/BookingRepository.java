package com.hotel.booking.repository;

import com.hotel.booking.dto.analytics.HotelAnalyticsDto;
import com.hotel.booking.dto.analytics.RevenueByDateDto;
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
    // total revenue (exclude cancelled)
    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Booking b WHERE b.status != 'CANCELLED'")
    Double getTotalRevenue();

    // total bookings
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status != 'CANCELLED'")
    Long getTotalBookings();

    // active bookings (confirmed + checked_in)
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status IN ('CONFIRMED', 'CHECKED_IN')")
    Long getActiveBookings();

    // completed bookings
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status = 'CHECKED_OUT'")
    Long getCompletedBookings();

    // cancelled bookings
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status = 'CANCELLED'")
    Long getCancelledBookings();

    // pending payments
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.paymentStatus = 'PENDING' AND b.status != 'CANCELLED'")
    Long getPendingPayments();

    // average booking value
    @Query("SELECT AVG(b.totalAmount) FROM Booking b WHERE b.status != 'CANCELLED'")
    Double getAverageBookingValue();

    // revenue by hotel
    @Query("""
    SELECT new com.hotel.booking.dto.analytics.HotelAnalyticsDto(
        b.hotelId,
        COUNT(b),
        COALESCE(SUM(b.totalAmount), 0),
        SUM(CASE WHEN b.status IN ('CONFIRMED', 'CHECKED_IN') THEN 1 ELSE 0 END)
    )
    FROM Booking b
    WHERE b.status != 'CANCELLED'
    GROUP BY b.hotelId
    ORDER BY SUM(b.totalAmount) DESC
    """)
    List<HotelAnalyticsDto> getRevenueByHotel();

    // revenue by date range
    @Query("""
    SELECT new com.hotel.booking.dto.analytics.RevenueByDateDto(
        b.checkInDate,
        COUNT(b),
        COALESCE(SUM(b.totalAmount), 0)
    )
    FROM Booking b
    WHERE b.status != 'CANCELLED'
    AND b.checkInDate BETWEEN :startDate AND :endDate
    GROUP BY b.checkInDate
    ORDER BY b.checkInDate
    """)
    List<RevenueByDateDto> getRevenueByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // revenue for specific hotel by date
    @Query("""
    SELECT new com.hotel.booking.dto.analytics.RevenueByDateDto(
        b.checkInDate,
        COUNT(b),
        COALESCE(SUM(b.totalAmount), 0)
    )
    FROM Booking b
    WHERE b.hotelId = :hotelId
    AND b.status != 'CANCELLED'
    AND b.checkInDate BETWEEN :startDate AND :endDate
    GROUP BY b.checkInDate
    ORDER BY b.checkInDate
    """)
    List<RevenueByDateDto> getRevenueByDateRangeForHotel(
            @Param("hotelId") Long hotelId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // today's check-ins count
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.checkInDate = :date AND b.status = 'CONFIRMED'")
    Long getTodayCheckInsCount(@Param("date") LocalDate date);

    // today's check-outs count
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.checkOutDate = :date AND b.status = 'CHECKED_IN'")
    Long getTodayCheckOutsCount(@Param("date") LocalDate date);
}