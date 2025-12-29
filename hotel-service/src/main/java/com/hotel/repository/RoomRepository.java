package com.hotel.repository;

import com.hotel.entity.Room;
import com.hotel.enums.RoomStatus;
import com.hotel.enums.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    // Find all rooms by hotel
    List<Room> findByHotelId(Long hotelId);

    // Find rooms by hotel and status
    List<Room> findByHotelIdAndStatus(Long hotelId, RoomStatus status);

    // Find available rooms by hotel
    List<Room> findByHotelIdAndStatusAndIsActive(Long hotelId, RoomStatus status, Boolean isActive);

    // Find rooms by hotel and room type
    List<Room> findByHotelIdAndRoomType(Long hotelId, RoomType roomType);

    // Check if room number exists for a hotel
    boolean existsByHotelIdAndRoomNumber(Long hotelId, String roomNumber);

    // Find room by hotel and room number
    Optional<Room> findByHotelIdAndRoomNumber(Long hotelId, String roomNumber);

    // Count rooms by hotel
    long countByHotelId(Long hotelId);

    // Count available rooms by hotel
    long countByHotelIdAndStatus(Long hotelId, RoomStatus status);

    // Find rooms by price range
    List<Room> findByHotelIdAndPricePerNightBetweenAndStatus(
            Long hotelId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            RoomStatus status);

    // Custom query: Search rooms with filters
    @Query("SELECT r FROM Room r WHERE r.hotel.id = :hotelId " +
            "AND r.isActive = true " +
            "AND (:status IS NULL OR r.status = :status) " +
            "AND (:roomType IS NULL OR r.roomType = :roomType) " +
            "AND (:minPrice IS NULL OR r.pricePerNight >= :minPrice) " +
            "AND (:maxPrice IS NULL OR r.pricePerNight <= :maxPrice)")
    List<Room> searchRooms(@Param("hotelId") Long hotelId,
                           @Param("status") RoomStatus status,
                           @Param("roomType") RoomType roomType,
                           @Param("minPrice") BigDecimal minPrice,
                           @Param("maxPrice") BigDecimal maxPrice);

    // Custom query: Get room count by status for a hotel
    @Query("SELECT r.status, COUNT(r) FROM Room r WHERE r.hotel.id = :hotelId GROUP BY r.status")
    List<Object[]> getRoomStatusCountByHotel(@Param("hotelId") Long hotelId);
}