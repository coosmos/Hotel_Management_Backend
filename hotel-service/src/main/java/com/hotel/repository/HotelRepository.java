package com.hotel.repository;

import com.hotel.entity.Hotel;
import com.hotel.enums.HotelStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, Long> {

    List<Hotel> findByStatus(HotelStatus status);
    List<Hotel> findByStatusOrderByNameAsc(HotelStatus status);
    @Query("SELECT h FROM Hotel h WHERE h.status = :status AND h.availableRooms > 0")
    List<Hotel> findHotelsWithAvailableRooms(@Param("status") HotelStatus status);
    @Query("SELECT h FROM Hotel h WHERE h.status = :status " +
            "AND (:city IS NULL OR LOWER(h.city) = LOWER(:city)) "
            )
    List<Hotel> searchHotels(@Param("status") HotelStatus status,
                             @Param("city") String city
                            );
    boolean existsByIdAndStatus(Long id, HotelStatus status);
}
