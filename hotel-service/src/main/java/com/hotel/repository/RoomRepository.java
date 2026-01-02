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
    List<Room> findByHotelId(Long hotelId);
    List<Room> findByHotelIdAndStatusAndIsActive(Long hotelId, RoomStatus status, Boolean isActive);
    boolean existsByHotelIdAndRoomNumber(Long hotelId, String roomNumber);
    long countByHotelId(Long hotelId);
    long countByHotelIdAndStatus(Long hotelId, RoomStatus status);
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

    @Query("SELECT r.status, COUNT(r) FROM Room r WHERE r.hotel.id = :hotelId GROUP BY r.status")
    List<Object[]> getRoomStatusCountByHotel(@Param("hotelId") Long hotelId);
}