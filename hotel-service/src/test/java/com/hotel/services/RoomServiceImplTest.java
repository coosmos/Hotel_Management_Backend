package com.hotel.service;

import com.hotel.dto.request.RoomRequestDto;
import com.hotel.dto.request.UpdateRoomStatusDto;
import com.hotel.dto.response.RoomResponseDto;
import com.hotel.entity.Hotel;
import com.hotel.entity.Room;
import com.hotel.enums.HotelStatus;
import com.hotel.enums.RoomStatus;
import com.hotel.enums.RoomType;
import com.hotel.enums.UserRole;
import com.hotel.exception.BadRequestException;
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

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoomService Tests")
class RoomServiceImplTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private HotelService hotelService;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private RoomServiceImpl roomService;

    private Hotel testHotel;
    private Room testRoom;
    private RoomRequestDto requestDto;
    private RoomResponseDto responseDto;
    private UpdateRoomStatusDto statusDto;

    @BeforeEach
    void setUp() {
        testHotel = new Hotel();
        testHotel.setId(1L);
        testHotel.setName("Test Hotel");
        testHotel.setStatus(HotelStatus.ACTIVE);

        testRoom = new Room();
        testRoom.setId(1L);
        testRoom.setHotel(testHotel);
        testRoom.setRoomNumber("101");
        testRoom.setRoomType(RoomType.DELUXE);
        testRoom.setPricePerNight(new BigDecimal("5000"));
        testRoom.setMaxOccupancy(2);
        testRoom.setStatus(RoomStatus.AVAILABLE);
        testRoom.setIsActive(true);

        requestDto = new RoomRequestDto();
        requestDto.setHotelId(1L);
        requestDto.setRoomNumber("101");
        requestDto.setRoomType(RoomType.DELUXE);
        requestDto.setPricePerNight(new BigDecimal("5000"));
        requestDto.setMaxOccupancy(2);

        responseDto = new RoomResponseDto();
        responseDto.setId(1L);
        responseDto.setHotelId(1L);
        responseDto.setRoomNumber("101");
        responseDto.setRoomType(RoomType.DELUXE);
        responseDto.setPricePerNight(new BigDecimal("5000"));

        statusDto = new UpdateRoomStatusDto();
        statusDto.setStatus(RoomStatus.OCCUPIED);
    }

    @Test
    @DisplayName("Should create room successfully")
    void testCreateRoom_Success() {
        // given
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(testHotel));
        when(roomRepository.existsByHotelIdAndRoomNumber(1L, "101")).thenReturn(false);
        when(modelMapper.map(requestDto, Room.class)).thenReturn(testRoom);
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);
        when(modelMapper.map(testRoom, RoomResponseDto.class)).thenReturn(responseDto);
        doNothing().when(hotelService).updateHotelRoomCounts(1L);

        // when
        RoomResponseDto result = roomService.createRoom(requestDto, 1L, UserRole.ADMIN.name(), null);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getRoomNumber()).isEqualTo("101");
        verify(roomRepository).save(any(Room.class));
        verify(hotelService).updateHotelRoomCounts(1L);
    }

    @Test
    @DisplayName("Should throw exception when hotel not found")
    void testCreateRoom_HotelNotFound() {
        // given
        when(hotelRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        requestDto.setHotelId(999L);
        assertThrows(ResourceNotFoundException.class, () ->
                roomService.createRoom(requestDto, 1L, UserRole.ADMIN.name(), null)
        );
        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    @DisplayName("Should throw exception when room number already exists")
    void testCreateRoom_DuplicateRoomNumber() {
        // given
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(testHotel));
        when(roomRepository.existsByHotelIdAndRoomNumber(1L, "101")).thenReturn(true);

        // when & then
        assertThrows(BadRequestException.class, () ->
                roomService.createRoom(requestDto, 1L, UserRole.ADMIN.name(), null)
        );
        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    @DisplayName("Should throw exception when MANAGER creates room for different hotel")
    void testCreateRoom_ManagerForbidden() {
        // when & then
        assertThrows(ForbiddenException.class, () ->
                roomService.createRoom(requestDto, 2L, UserRole.MANAGER.name(), 2L) // userHotelId = 2, but requestDto.hotelId = 1
        );
        verify(roomRepository, never()).save(any(Room.class));
    }

    @Test
    @DisplayName("Should update room successfully")
    void testUpdateRoom_Success() {
        // given
        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);
        when(modelMapper.map(testRoom, RoomResponseDto.class)).thenReturn(responseDto);
        doNothing().when(hotelService).updateHotelRoomCounts(1L);

        // when
        RoomResponseDto result = roomService.updateRoom(1L, requestDto, 1L, UserRole.ADMIN.name(), null);

        // then
        assertThat(result).isNotNull();
        verify(roomRepository).save(any(Room.class));
        verify(hotelService).updateHotelRoomCounts(1L);
    }

    @Test
    @DisplayName("Should update room status successfully")
    void testUpdateRoomStatus_Success() {
        // given
        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);
        when(modelMapper.map(testRoom, RoomResponseDto.class)).thenReturn(responseDto);
        doNothing().when(hotelService).updateHotelRoomCounts(1L);

        // when
        RoomResponseDto result = roomService.updateRoomStatus(1L, statusDto, 1L, UserRole.MANAGER.name(), 1L);

        // then
        assertThat(result).isNotNull();
        verify(roomRepository).save(any(Room.class));
        verify(hotelService).updateHotelRoomCounts(1L);
    }

    @Test
    @DisplayName("Should get room by id successfully")
    void testGetRoomById_Success() {
        // given
        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(modelMapper.map(testRoom, RoomResponseDto.class)).thenReturn(responseDto);

        // when
        RoomResponseDto result = roomService.getRoomById(1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should get available rooms by hotel id")
    void testGetAvailableRoomsByHotelId() {
        // given
        List<Room> rooms = Arrays.asList(testRoom);
        when(roomRepository.findByHotelIdAndStatusAndIsActive(1L, RoomStatus.AVAILABLE, true))
                .thenReturn(rooms);
        when(modelMapper.map(any(Room.class), eq(RoomResponseDto.class))).thenReturn(responseDto);

        // when
        List<RoomResponseDto> result = roomService.getAvailableRoomsByHotelId(1L);

        // then
        assertThat(result).hasSize(1);
        verify(roomRepository).findByHotelIdAndStatusAndIsActive(1L, RoomStatus.AVAILABLE, true);
    }

    @Test
    @DisplayName("Should delete room - ADMIN success")
    void testDeleteRoom_AdminSuccess() {
        // given
        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        doNothing().when(roomRepository).delete(testRoom);
        doNothing().when(hotelService).updateHotelRoomCounts(1L);

        // when
        roomService.deleteRoom(1L, 1L, UserRole.ADMIN.name(), null);

        // then
        verify(roomRepository).delete(testRoom);
        verify(hotelService).updateHotelRoomCounts(1L);
    }

    @Test
    @DisplayName("Should throw exception when RECEPTIONIST tries to delete room")
    void testDeleteRoom_ReceptionistForbidden() {
        // given
        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));

        // when & then
        assertThrows(ForbiddenException.class, () ->
                roomService.deleteRoom(1L, 1L, UserRole.RECEPTIONIST.name(), 1L)
        );
        verify(roomRepository, never()).delete(any(Room.class));
    }
}