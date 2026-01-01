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
    // Find hotels by city
    List<Hotel> findByCityAndStatus(String city, HotelStatus status);
    // Find hotels by city (case-insensitive)
    List<Hotel> findByCityIgnoreCaseAndStatus(String city, HotelStatus status);
    // Find hotels by state
    List<Hotel> findByStateAndStatus(String state, HotelStatus status);
    // Search hotels by name (case-insensitive, partial match)
    List<Hotel> findByNameContainingIgnoreCaseAndStatus(String name, HotelStatus status);
    // Find hotels by star rating
    List<Hotel> findByStarRatingGreaterThanEqualAndStatus(Integer starRating, HotelStatus status);
    //Find hotels with available rooms
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
