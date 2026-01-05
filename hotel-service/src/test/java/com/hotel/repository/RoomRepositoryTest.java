package com.hotel.repository;

import com.hotel.entity.Hotel;
import com.hotel.entity.Room;
import com.hotel.enums.HotelStatus;
import com.hotel.enums.RoomStatus;
import com.hotel.enums.RoomType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("RoomRepository Tests")
class RoomRepositoryTest {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Hotel testHotel;
    private Room availableRoom;
    private Room occupiedRoom;

    @BeforeEach
    void setUp() {
        // create hotel
        testHotel = new Hotel();
        testHotel.setName("Test Hotel");
        testHotel.setCity("Mumbai");
        testHotel.setState("Maharashtra");
        testHotel.setCountry("India");
        testHotel.setAddress("Test Address");
        testHotel.setStatus(HotelStatus.ACTIVE);
        entityManager.persist(testHotel);

        // create available room
        availableRoom = new Room();
        availableRoom.setHotel(testHotel);
        availableRoom.setRoomNumber("101");
        availableRoom.setRoomType(RoomType.DELUXE);
        availableRoom.setPricePerNight(new BigDecimal("5000"));
        availableRoom.setMaxOccupancy(2);
        availableRoom.setStatus(RoomStatus.AVAILABLE);
        availableRoom.setIsActive(true);
        entityManager.persist(availableRoom);

        // create occupied room
        occupiedRoom = new Room();
        occupiedRoom.setHotel(testHotel);
        occupiedRoom.setRoomNumber("102");
        occupiedRoom.setRoomType(RoomType.SUITE);
        occupiedRoom.setPricePerNight(new BigDecimal("8000"));
        occupiedRoom.setMaxOccupancy(4);
        occupiedRoom.setStatus(RoomStatus.OCCUPIED);
        occupiedRoom.setIsActive(true);
        entityManager.persist(occupiedRoom);

        entityManager.flush();
    }

    @Test
    @DisplayName("Should find rooms by hotel id")
    void testFindByHotelId() {
        // when
        List<Room> rooms = roomRepository.findByHotelId(testHotel.getId());

        // then
        assertThat(rooms).hasSize(2);
    }

    @Test
    @DisplayName("Should find only available and active rooms")
    void testFindByHotelIdAndStatusAndIsActive() {
        // when
        List<Room> rooms = roomRepository.findByHotelIdAndStatusAndIsActive(
                testHotel.getId(), RoomStatus.AVAILABLE, true);

        // then
        assertThat(rooms).hasSize(1);
        assertThat(rooms.get(0).getRoomNumber()).isEqualTo("101");
    }

    @Test
    @DisplayName("Should check if room number exists for hotel")
    void testExistsByHotelIdAndRoomNumber() {
        // when
        boolean exists = roomRepository.existsByHotelIdAndRoomNumber(testHotel.getId(), "101");
        boolean notExists = roomRepository.existsByHotelIdAndRoomNumber(testHotel.getId(), "999");

        // then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Should count rooms by hotel id")
    void testCountByHotelId() {
        // when
        long count = roomRepository.countByHotelId(testHotel.getId());

        // then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Should count available rooms")
    void testCountByHotelIdAndStatus() {
        // when
        long availableCount = roomRepository.countByHotelIdAndStatus(testHotel.getId(), RoomStatus.AVAILABLE);
        long occupiedCount = roomRepository.countByHotelIdAndStatus(testHotel.getId(), RoomStatus.OCCUPIED);

        // then
        assertThat(availableCount).isEqualTo(1);
        assertThat(occupiedCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Should search rooms with filters")
    void testSearchRooms() {
        // when - search by room type
        List<Room> deluxeRooms = roomRepository.searchRooms(
                testHotel.getId(), null, RoomType.DELUXE, null, null);

        // then
        assertThat(deluxeRooms).hasSize(1);
        assertThat(deluxeRooms.get(0).getRoomType()).isEqualTo(RoomType.DELUXE);
    }

    @Test
    @DisplayName("Should search rooms by price range")
    void testSearchRooms_PriceRange() {
        // when - price between 4000 and 6000
        List<Room> rooms = roomRepository.searchRooms(
                testHotel.getId(), null, null, new BigDecimal("4000"), new BigDecimal("6000"));

        // then
        assertThat(rooms).hasSize(1);
        assertThat(rooms.get(0).getRoomNumber()).isEqualTo("101");
    }
}