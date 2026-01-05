package com.hotel.repository;

import com.hotel.entity.Hotel;
import com.hotel.enums.HotelStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("HotelRepository Tests")
class HotelRepositoryTest {

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Hotel activeHotel1;
    private Hotel activeHotel2;
    private Hotel inactiveHotel;

    @BeforeEach
    void setUp() {
        // create active hotel in Mumbai
        activeHotel1 = new Hotel();
        activeHotel1.setName("Grand Mumbai Hotel");
        activeHotel1.setDescription("Luxury hotel in Mumbai");
        activeHotel1.setAddress("123 Marine Drive");
        activeHotel1.setCity("Mumbai");
        activeHotel1.setState("Maharashtra");
        activeHotel1.setCountry("India");
        activeHotel1.setPincode("400001");
        activeHotel1.setContactNumber("1234567890");
        activeHotel1.setEmail("mumbai@hotel.com");
        activeHotel1.setStarRating(5);
        activeHotel1.setAmenities("WiFi,Pool,Gym");
        activeHotel1.setStatus(HotelStatus.ACTIVE);
        activeHotel1.setTotalRooms(10);
        activeHotel1.setAvailableRooms(5);
        entityManager.persist(activeHotel1);

        // create another active hotel in Delhi
        activeHotel2 = new Hotel();
        activeHotel2.setName("Delhi Palace");
        activeHotel2.setDescription("Heritage hotel in Delhi");
        activeHotel2.setAddress("456 Connaught Place");
        activeHotel2.setCity("Delhi");
        activeHotel2.setState("Delhi");
        activeHotel2.setCountry("India");
        activeHotel2.setPincode("110001");
        activeHotel2.setContactNumber("9876543210");
        activeHotel2.setEmail("delhi@hotel.com");
        activeHotel2.setStarRating(4);
        activeHotel2.setAmenities("WiFi,Restaurant");
        activeHotel2.setStatus(HotelStatus.ACTIVE);
        activeHotel2.setTotalRooms(0);
        activeHotel2.setAvailableRooms(0);
        entityManager.persist(activeHotel2);

        // create inactive hotel
        inactiveHotel = new Hotel();
        inactiveHotel.setName("Closed Hotel");
        inactiveHotel.setDescription("Under renovation");
        inactiveHotel.setAddress("789 Old Street");
        inactiveHotel.setCity("Bangalore");
        inactiveHotel.setState("Karnataka");
        inactiveHotel.setCountry("India");
        inactiveHotel.setPincode("560001");
        inactiveHotel.setContactNumber("5555555555");
        inactiveHotel.setEmail("closed@hotel.com");
        inactiveHotel.setStarRating(3);
        inactiveHotel.setAmenities("WiFi");
        inactiveHotel.setStatus(HotelStatus.INACTIVE);
        inactiveHotel.setTotalRooms(5);
        inactiveHotel.setAvailableRooms(0);
        entityManager.persist(inactiveHotel);

        entityManager.flush();
    }

    // ==================== FIND BY STATUS TESTS ====================

    @Test
    @DisplayName("Should find hotels by ACTIVE status")
    void testFindByStatus_Active() {
        // when
        List<Hotel> activeHotels = hotelRepository.findByStatus(HotelStatus.ACTIVE);

        // then
        assertThat(activeHotels).hasSize(2);
        assertThat(activeHotels).extracting(Hotel::getName)
                .containsExactlyInAnyOrder("Grand Mumbai Hotel", "Delhi Palace");
    }

    @Test
    @DisplayName("Should find hotels by INACTIVE status")
    void testFindByStatus_Inactive() {
        // when
        List<Hotel> inactiveHotels = hotelRepository.findByStatus(HotelStatus.INACTIVE);

        // then
        assertThat(inactiveHotels).hasSize(1);
        assertThat(inactiveHotels.get(0).getName()).isEqualTo("Closed Hotel");
    }

    @Test
    @DisplayName("Should return empty list when no hotels with given status")
    void testFindByStatus_NoResults() {
        // when
        List<Hotel> maintenanceHotels = hotelRepository.findByStatus(HotelStatus.UNDER_MAINTENANCE);

        // then
        assertThat(maintenanceHotels).isEmpty();
    }

    // ==================== FIND BY STATUS ORDERED TESTS ====================

    @Test
    @DisplayName("Should find active hotels ordered by name ascending")
    void testFindByStatusOrderByNameAsc() {
        // when
        List<Hotel> hotels = hotelRepository.findByStatusOrderByNameAsc(HotelStatus.ACTIVE);

        // then
        assertThat(hotels).hasSize(2);
        assertThat(hotels.get(0).getName()).isEqualTo("Delhi Palace");
        assertThat(hotels.get(1).getName()).isEqualTo("Grand Mumbai Hotel");
    }

    // ==================== FIND HOTELS WITH AVAILABLE ROOMS TESTS ====================

    @Test
    @DisplayName("Should find active hotels with available rooms")
    void testFindHotelsWithAvailableRooms() {
        // when
        List<Hotel> hotelsWithRooms = hotelRepository.findHotelsWithAvailableRooms(HotelStatus.ACTIVE);

        // then
        assertThat(hotelsWithRooms).hasSize(1);
        assertThat(hotelsWithRooms.get(0).getName()).isEqualTo("Grand Mumbai Hotel");
        assertThat(hotelsWithRooms.get(0).getAvailableRooms()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should not include hotels with zero available rooms")
    void testFindHotelsWithAvailableRooms_ExcludesZero() {
        // when
        List<Hotel> hotelsWithRooms = hotelRepository.findHotelsWithAvailableRooms(HotelStatus.ACTIVE);

        // then
        assertThat(hotelsWithRooms).doesNotContain(activeHotel2); // Delhi Palace has 0 available rooms
    }

    // ==================== SEARCH HOTELS TESTS ====================

    @Test
    @DisplayName("Should search hotels by city")
    void testSearchHotels_ByCity() {
        // when
        List<Hotel> mumbaiHotels = hotelRepository.searchHotels(HotelStatus.ACTIVE, "Mumbai");

        // then
        assertThat(mumbaiHotels).hasSize(1);
        assertThat(mumbaiHotels.get(0).getName()).isEqualTo("Grand Mumbai Hotel");
        assertThat(mumbaiHotels.get(0).getCity()).isEqualToIgnoringCase("Mumbai");
    }

    @Test
    @DisplayName("Should search hotels with city case insensitive")
    void testSearchHotels_CityInsensitive() {
        // when
        List<Hotel> mumbaiHotels = hotelRepository.searchHotels(HotelStatus.ACTIVE, "mumbai");

        // then
        assertThat(mumbaiHotels).hasSize(1);
        assertThat(mumbaiHotels.get(0).getCity()).isEqualToIgnoringCase("mumbai");
    }

    @Test
    @DisplayName("Should search all active hotels when city is null")
    void testSearchHotels_NullCity() {
        // when
        List<Hotel> allActiveHotels = hotelRepository.searchHotels(HotelStatus.ACTIVE, null);

        // then
        assertThat(allActiveHotels).hasSize(2);
        assertThat(allActiveHotels).extracting(Hotel::getName)
                .containsExactlyInAnyOrder("Grand Mumbai Hotel", "Delhi Palace");
    }

    @Test
    @DisplayName("Should only return active hotels in search")
    void testSearchHotels_OnlyActive() {
        // when
        List<Hotel> hotels = hotelRepository.searchHotels(HotelStatus.ACTIVE, null);

        // then
        assertThat(hotels).hasSize(2);
        assertThat(hotels).extracting(Hotel::getStatus)
                .containsOnly(HotelStatus.ACTIVE);
        assertThat(hotels).doesNotContain(inactiveHotel);
    }

    @Test
    @DisplayName("Should return empty list when city not found")
    void testSearchHotels_CityNotFound() {
        // when
        List<Hotel> hotels = hotelRepository.searchHotels(HotelStatus.ACTIVE, "NonExistentCity");

        // then
        assertThat(hotels).isEmpty();
    }

    // ==================== EXISTS BY ID AND STATUS TESTS ====================

    @Test
    @DisplayName("Should return true when hotel exists with given id and status")
    void testExistsByIdAndStatus_True() {
        // when
        boolean exists = hotelRepository.existsByIdAndStatus(activeHotel1.getId(), HotelStatus.ACTIVE);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false when hotel id exists but status doesn't match")
    void testExistsByIdAndStatus_WrongStatus() {
        // when
        boolean exists = hotelRepository.existsByIdAndStatus(activeHotel1.getId(), HotelStatus.INACTIVE);

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should return false when hotel id doesn't exist")
    void testExistsByIdAndStatus_IdNotFound() {
        // when
        boolean exists = hotelRepository.existsByIdAndStatus(999L, HotelStatus.ACTIVE);

        // then
        assertThat(exists).isFalse();
    }

    // ==================== SAVE AND UPDATE TESTS ====================

    @Test
    @DisplayName("Should save hotel with all fields")
    void testSaveHotel_Success() {
        // given
        Hotel newHotel = new Hotel();
        newHotel.setName("Test Hotel");
        newHotel.setDescription("Test Description");
        newHotel.setAddress("Test Address");
        newHotel.setCity("Test City");
        newHotel.setState("Test State");
        newHotel.setCountry("Test Country");
        newHotel.setPincode("123456");
        newHotel.setContactNumber("1111111111");
        newHotel.setEmail("test@hotel.com");
        newHotel.setStarRating(3);
        newHotel.setAmenities("WiFi");
        newHotel.setStatus(HotelStatus.ACTIVE);
        newHotel.setTotalRooms(20);
        newHotel.setAvailableRooms(15);

        // when
        Hotel saved = hotelRepository.save(newHotel);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Test Hotel");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should update hotel status to INACTIVE")
    void testUpdateHotelStatus_ToInactive() {
        // given
        Hotel hotel = hotelRepository.findById(activeHotel1.getId()).orElseThrow();
        hotel.setStatus(HotelStatus.INACTIVE);

        // when
        Hotel updated = hotelRepository.save(hotel);

        // then
        assertThat(updated.getStatus()).isEqualTo(HotelStatus.INACTIVE);
    }
}