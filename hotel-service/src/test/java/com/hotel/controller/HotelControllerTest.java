package com.hotel.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.dto.request.HotelRequestDto;
import com.hotel.dto.response.HotelResponseDto;
import com.hotel.enums.HotelStatus;
import com.hotel.enums.UserRole;
import com.hotel.exception.ResourceNotFoundException;
import com.hotel.service.HotelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HotelController.class)
@DisplayName("HotelController Tests")
class HotelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private HotelService hotelService;

    private HotelRequestDto requestDto;
    private HotelResponseDto responseDto;

    @BeforeEach
    void setUp() {
        requestDto = new HotelRequestDto();
        requestDto.setName("Test Hotel");
        requestDto.setCity("Mumbai");
        requestDto.setState("Maharashtra");
        requestDto.setCountry("India");
        requestDto.setAddress("Test Address");
        requestDto.setContactNumber("1234567890");
        requestDto.setEmail("test@hotel.com");
        requestDto.setStarRating(5);

        responseDto = new HotelResponseDto();
        responseDto.setId(1L);
        responseDto.setName("Test Hotel");
        responseDto.setCity("Mumbai");
        responseDto.setStatus(HotelStatus.ACTIVE);
    }

    @Test
    @DisplayName("POST /api/hotels - Create hotel by ADMIN - Success")
    void testCreateHotel_AdminSuccess() throws Exception {
        // given
        when(hotelService.createHotel(any(HotelRequestDto.class), eq(1L))).thenReturn(responseDto);

        // when & then
        mockMvc.perform(post("/api/hotels")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", UserRole.ADMIN.name())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Test Hotel"))
                .andExpect(jsonPath("$.data.city").value("Mumbai"));

        verify(hotelService, times(1)).createHotel(any(HotelRequestDto.class), eq(1L));
    }

    @Test
    @DisplayName("POST /api/hotels - Create hotel by GUEST - Forbidden")
    void testCreateHotel_GuestForbidden() throws Exception {
        // when & then
        mockMvc.perform(post("/api/hotels")
                        .header("X-User-Id", "2")
                        .header("X-User-Role", UserRole.GUEST.name())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isForbidden());

        verify(hotelService, never()).createHotel(any(HotelRequestDto.class), anyLong());
    }

    @Test
    @DisplayName("PUT /api/hotels/{id} - Update hotel - Success")
    void testUpdateHotel_Success() throws Exception {
        // given
        when(hotelService.updateHotel(eq(1L), any(HotelRequestDto.class), eq(1L),
                eq(UserRole.ADMIN.name()), isNull())).thenReturn(responseDto);

        // when & then
        mockMvc.perform(put("/api/hotels/1")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", UserRole.ADMIN.name())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Test Hotel"));

        verify(hotelService, times(1)).updateHotel(eq(1L), any(HotelRequestDto.class),
                eq(1L), eq(UserRole.ADMIN.name()), isNull());
    }

    @Test
    @DisplayName("GET /api/hotels/{id} - Get hotel by ID - Success")
    void testGetHotelById_Success() throws Exception {
        // given
        when(hotelService.getHotelById(1L)).thenReturn(responseDto);

        // when & then
        mockMvc.perform(get("/api/hotels/1")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Test Hotel"));

        verify(hotelService, times(1)).getHotelById(1L);
    }

    @Test
    @DisplayName("GET /api/hotels/{id} - Hotel not found")
    void testGetHotelById_NotFound() throws Exception {
        // given
        when(hotelService.getHotelById(999L)).thenThrow(new ResourceNotFoundException("Hotel", "id", 999L));

        // when & then
        mockMvc.perform(get("/api/hotels/999")
                        .header("X-User-Id", "1"))
                .andExpect(status().isNotFound());

        verify(hotelService, times(1)).getHotelById(999L);
    }

    @Test
    @DisplayName("GET /api/hotels - Get all hotels - ADMIN only")
    void testGetAllHotels_AdminSuccess() throws Exception {
        // given
        List<HotelResponseDto> hotels = Arrays.asList(responseDto);
        when(hotelService.getAllHotels()).thenReturn(hotels);

        // when & then
        mockMvc.perform(get("/api/hotels")
                        .header("X-User-Role", UserRole.ADMIN.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("Test Hotel"));

        verify(hotelService, times(1)).getAllHotels();
    }

    @Test
    @DisplayName("GET /api/hotels/search - Search hotels by city")
    void testSearchHotels_Success() throws Exception {
        // given
        List<HotelResponseDto> hotels = Arrays.asList(responseDto);
        when(hotelService.searchHotels("Mumbai")).thenReturn(hotels);

        // when & then
        mockMvc.perform(get("/api/hotels/search")
                        .param("city", "Mumbai")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].city").value("Mumbai"));

        verify(hotelService, times(1)).searchHotels("Mumbai");
    }

    @Test
    @DisplayName("GET /api/hotels/my-hotel - Get my hotel - MANAGER success")
    void testGetMyHotel_ManagerSuccess() throws Exception {
        // given
        when(hotelService.getMyHotel(1L)).thenReturn(responseDto);

        // when & then
        mockMvc.perform(get("/api/hotels/my-hotel")
                        .header("X-User-Id", "1")
                        .header("X-User-Role", UserRole.MANAGER.name())
                        .header("X-Hotel-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Test Hotel"));

        verify(hotelService, times(1)).getMyHotel(1L);
    }
}