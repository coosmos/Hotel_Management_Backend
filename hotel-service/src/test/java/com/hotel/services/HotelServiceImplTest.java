package com.hotel.service;

import com.hotel.dto.request.HotelRequestDto;
import com.hotel.dto.response.HotelResponseDto;
import com.hotel.entity.Hotel;
import com.hotel.enums.HotelStatus;
import com.hotel.enums.RoomStatus;
import com.hotel.enums.UserRole;
import com.hotel.exception.ForbiddenException;
import com.hotel.exception.ResourceNotFoundException;
import com.hotel.repository.HotelRepository;
import com.hotel.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HotelService Tests")
class HotelServiceImplTest {

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private HotelServiceImpl hotelService;

    private Hotel testHotel;
    private HotelRequestDto requestDto;
    private HotelResponseDto responseDto;

    @BeforeEach
    void setUp() {
        testHotel = new Hotel();
        testHotel.setId(1L);
        testHotel.setName("Test Hotel");
        testHotel.setCity("Mumbai");
        testHotel.setState("Maharashtra");
        testHotel.setCountry("India");
        testHotel.setAddress("Test Address");
        testHotel.setStatus(HotelStatus.ACTIVE);
        testHotel.setTotalRooms(10);
        testHotel.setAvailableRooms(5);

        requestDto = new HotelRequestDto();
        requestDto.setName("Test Hotel");
        requestDto.setCity("Mumbai");
        requestDto.setState("Maharashtra");
        requestDto.setCountry("India");
        requestDto.setAddress("Test Address");

        responseDto = new HotelResponseDto();
        responseDto.setId(1L);
        responseDto.setName("Test Hotel");
        responseDto.setCity("Mumbai");
    }

    @Test
    @DisplayName("Should create hotel successfully")
    void testCreateHotel_Success() {
        // given
        when(modelMapper.map(requestDto, Hotel.class)).thenReturn(testHotel);
        when(hotelRepository.save(any(Hotel.class))).thenReturn(testHotel);
        when(modelMapper.map(testHotel, HotelResponseDto.class)).thenReturn(responseDto);

        // when
        HotelResponseDto result = hotelService.createHotel(requestDto, 1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Hotel");
        verify(hotelRepository).save(any(Hotel.class));
    }

    @Test
    @DisplayName("Should update hotel successfully by ADMIN")
    void testUpdateHotel_AdminSuccess() {
        // given
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(testHotel));
        when(hotelRepository.save(any(Hotel.class))).thenReturn(testHotel);
        when(modelMapper.map(testHotel, HotelResponseDto.class)).thenReturn(responseDto);

        // when
        HotelResponseDto result = hotelService.updateHotel(1L, requestDto, 1L, UserRole.ADMIN.name(), null);

        // then
        assertThat(result).isNotNull();
        verify(hotelRepository).save(any(Hotel.class));
    }

    @Test
    @DisplayName("Should throw exception when MANAGER tries to update different hotel")
    void testUpdateHotel_ManagerForbidden() {
        // given
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(testHotel));

        // when & then
        assertThrows(ForbiddenException.class, () ->
                hotelService.updateHotel(1L, requestDto, 2L, UserRole.MANAGER.name(), 2L)
        );
        verify(hotelRepository, never()).save(any(Hotel.class));
    }

    @Test
    @DisplayName("Should get hotel by id successfully")
    void testGetHotelById_Success() {
        // given
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(testHotel));
        when(modelMapper.map(testHotel, HotelResponseDto.class)).thenReturn(responseDto);

        // when
        HotelResponseDto result = hotelService.getHotelById(1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should throw exception when hotel not found")
    void testGetHotelById_NotFound() {
        // given
        when(hotelRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThrows(ResourceNotFoundException.class, () ->
                hotelService.getHotelById(999L)
        );
    }

    @Test
    @DisplayName("Should search hotels by city")
    void testSearchHotels_ByCity() {
        // given
        List<Hotel> hotels = Arrays.asList(testHotel);
        when(hotelRepository.searchHotels(HotelStatus.ACTIVE, "Mumbai")).thenReturn(hotels);
        when(modelMapper.map(any(Hotel.class), eq(HotelResponseDto.class))).thenReturn(responseDto);

        // when
        List<HotelResponseDto> result = hotelService.searchHotels("Mumbai");

        // then
        assertThat(result).hasSize(1);
        verify(hotelRepository).searchHotels(HotelStatus.ACTIVE, "Mumbai");
    }

    @Test
    @DisplayName("Should delete hotel - only ADMIN")
    void testDeleteHotel_AdminSuccess() {
        // given
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(testHotel));
        when(hotelRepository.save(any(Hotel.class))).thenReturn(testHotel);

        // when
        hotelService.deleteHotel(1L, 1L, UserRole.ADMIN.name());

        // then
        verify(hotelRepository).save(testHotel);
        assertThat(testHotel.getStatus()).isEqualTo(HotelStatus.INACTIVE);
    }

    @Test
    @DisplayName("Should throw exception when non-ADMIN tries to delete hotel")
    void testDeleteHotel_ManagerForbidden() {
        // when & then
        assertThrows(ForbiddenException.class, () ->
                hotelService.deleteHotel(1L, 2L, UserRole.MANAGER.name())
        );
        verify(hotelRepository, never()).save(any(Hotel.class));
    }

    @Test
    @DisplayName("Should update hotel room counts")
    void testUpdateHotelRoomCounts() {
        // given
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(testHotel));
        when(roomRepository.countByHotelId(1L)).thenReturn(10L);
        when(roomRepository.countByHotelIdAndStatus(1L, RoomStatus.AVAILABLE)).thenReturn(5L);
        when(hotelRepository.save(any(Hotel.class))).thenReturn(testHotel);

        // when
        hotelService.updateHotelRoomCounts(1L);

        // then
        verify(hotelRepository).save(testHotel);
        assertThat(testHotel.getTotalRooms()).isEqualTo(10);
        assertThat(testHotel.getAvailableRooms()).isEqualTo(5);
    }
}