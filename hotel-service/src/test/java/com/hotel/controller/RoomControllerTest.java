package com.hotel.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.dto.request.RoomRequestDto;
import com.hotel.dto.request.UpdateRoomStatusDto;
import com.hotel.dto.response.RoomResponseDto;
import com.hotel.enums.RoomStatus;
import com.hotel.enums.RoomType;
import com.hotel.enums.UserRole;
import com.hotel.exception.BadRequestException;
import com.hotel.exception.ForbiddenException;
import com.hotel.exception.ResourceNotFoundException;
import com.hotel.service.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RoomController.class)
@DisplayName("RoomController Tests")
class RoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RoomService roomService;

    private RoomRequestDto requestDto;
    private RoomResponseDto responseDto;
    private UpdateRoomStatusDto statusDto;

    @BeforeEach
    void setUp() {
        requestDto = new RoomRequestDto();
        requestDto.setHotelId(1L);
        requestDto.setRoomNumber("101");
        requestDto.setRoomType(RoomType.DELUXE);
        requestDto.setPricePerNight(new BigDecimal("5000"));
        requestDto.setMaxOccupancy(2);
        requestDto.setFloorNumber(1);
        requestDto.setBedType("King");
        requestDto.setRoomSize(400);
        requestDto.setAmenities("WiFi,AC,TV");
        requestDto.setDescription("Luxury room");

        responseDto = new RoomResponseDto();
        responseDto.setId(1L);
        responseDto.setHotelId(1L);
        responseDto.setRoomNumber("101");
        responseDto.setRoomType(RoomType.DELUXE);
        responseDto.setPricePerNight(new BigDecimal("5000"));
        responseDto.setStatus(RoomStatus.AVAILABLE);

        statusDto = new UpdateRoomStatusDto();
        statusDto.setStatus(RoomStatus.OCCUPIED);
    }

    @Test
    @DisplayName("POST /api/hotels/rooms - Create room - ADMIN success")
    void testCreateRoom_AdminSuccess() throws Exception {
        // given
        when(roomService.createRoom(any(RoomRequestDto.class), eq(1L),
                eq(UserRole.ADMIN.name()), isNull())).thenReturn(responseDto);

        // when & then
        mockMvc.perform(post("/api/hotels/rooms")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", UserRole.ADMIN.name())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomNumber").value("101"))
                .andExpect(jsonPath("$.roomType").value("DELUXE"));

        verify(roomService, times(1)).createRoom(any(RoomRequestDto.class), eq(1L),
                eq(UserRole.ADMIN.name()), isNull());
    }

    @Test
    @DisplayName("POST /api/hotels/rooms - Create room - Duplicate room number")
    void testCreateRoom_DuplicateRoomNumber() throws Exception {
        // given
        when(roomService.createRoom(any(RoomRequestDto.class), eq(1L),
                eq(UserRole.ADMIN.name()), isNull()))
                .thenThrow(new BadRequestException("Room number 101 already exists for this hotel"));

        // when & then
        mockMvc.perform(post("/api/hotels/rooms")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", UserRole.ADMIN.name())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());

        verify(roomService, times(1)).createRoom(any(RoomRequestDto.class), eq(1L),
                eq(UserRole.ADMIN.name()), isNull());
    }

    @Test
    @DisplayName("PUT /api/hotels/rooms/{id} - Update room - Success")
    void testUpdateRoom_Success() throws Exception {
        // given
        when(roomService.updateRoom(eq(1L), any(RoomRequestDto.class), eq(1L),
                eq(UserRole.ADMIN.name()), isNull())).thenReturn(responseDto);

        // when & then
        mockMvc.perform(put("/api/hotels/rooms/1")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", UserRole.ADMIN.name())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomNumber").value("101"));

        verify(roomService, times(1)).updateRoom(eq(1L), any(RoomRequestDto.class),
                eq(1L), eq(UserRole.ADMIN.name()), isNull());
    }

    @Test
    @DisplayName("PATCH /api/hotels/rooms/{id}/status - Update room status - Success")
    void testUpdateRoomStatus_Success() throws Exception {
        // given
        when(roomService.updateRoomStatus(eq(1L), any(UpdateRoomStatusDto.class),
                eq(1L), eq(UserRole.MANAGER.name()), eq(1L))).thenReturn(responseDto);

        // when & then
        mockMvc.perform(patch("/api/hotels/rooms/1/status")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", UserRole.MANAGER.name())
                        .header("X-Hotel-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomNumber").value("101"));

        verify(roomService, times(1)).updateRoomStatus(eq(1L),
                any(UpdateRoomStatusDto.class), eq(1L), eq(UserRole.MANAGER.name()), eq(1L));
    }

    @Test
    @DisplayName("GET /api/hotels/rooms/{id} - Get room by ID - Success")
    void testGetRoomById_Success() throws Exception {
        // given
        when(roomService.getRoomById(1L)).thenReturn(responseDto);

        // when & then
        mockMvc.perform(get("/api/hotels/rooms/1")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.roomNumber").value("101"));

        verify(roomService, times(1)).getRoomById(1L);
    }

    @Test
    @DisplayName("GET /api/hotels/rooms/hotel/{hotelId}/available - Get available rooms")
    void testGetAvailableRoomsByHotelId() throws Exception {
        // given
        List<RoomResponseDto> rooms = Arrays.asList(responseDto);
        when(roomService.getAvailableRoomsByHotelId(1L)).thenReturn(rooms);

        // when & then
        mockMvc.perform(get("/api/hotels/rooms/hotel/1/available")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].roomNumber").value("101"))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));

        verify(roomService, times(1)).getAvailableRoomsByHotelId(1L);
    }

    @Test
    @DisplayName("GET /api/hotels/rooms/hotel/{hotelId}/search - Search rooms with filters")
    void testSearchRooms() throws Exception {
        // given
        List<RoomResponseDto> rooms = Arrays.asList(responseDto);
        when(roomService.searchRooms(eq(1L), eq(RoomStatus.AVAILABLE), eq(RoomType.DELUXE),
                any(), any())).thenReturn(rooms);

        // when & then
        mockMvc.perform(get("/api/hotels/rooms/hotel/1/search")
                        .param("status", "AVAILABLE")
                        .param("roomType", "DELUXE")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].roomType").value("DELUXE"));

        verify(roomService, times(1)).searchRooms(eq(1L), eq(RoomStatus.AVAILABLE),
                eq(RoomType.DELUXE), isNull(), isNull());
    }

    @Test
    @DisplayName("DELETE /api/hotels/rooms/{id} - Delete room - ADMIN success")
    void testDeleteRoom_AdminSuccess() throws Exception {
        // given
        doNothing().when(roomService).deleteRoom(eq(1L), eq(1L),
                eq(UserRole.ADMIN.name()), isNull());

        // when & then
        mockMvc.perform(delete("/api/hotels/rooms/1")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", UserRole.ADMIN.name()))
                .andExpect(status().isNoContent());

        verify(roomService, times(1)).deleteRoom(eq(1L), eq(1L),
                eq(UserRole.ADMIN.name()), isNull());
    }
}