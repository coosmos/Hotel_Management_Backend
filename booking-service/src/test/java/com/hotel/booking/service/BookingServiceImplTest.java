package com.hotel.booking.service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.hotel.booking.service.BookingServiceImpl;
import com.hotel.booking.service.KafkaProducerService;

import com.hotel.booking.repository.BookingRepository;

import com.hotel.booking.client.HotelServiceClient;

import com.hotel.booking.security.AuthorizationUtil;
import com.hotel.booking.security.UserContext;

import com.hotel.booking.dto.request.BookingCreateRequest;
import com.hotel.booking.dto.request.CheckInRequest;
import com.hotel.booking.dto.request.CheckOutRequest;

import com.hotel.booking.dto.response.*;
import com.hotel.booking.dto.external.RoomDto;
import com.hotel.booking.dto.external.HotelDto;

import com.hotel.booking.entity.Booking;

import com.hotel.booking.enums.BookingStatus;
import com.hotel.booking.enums.PaymentStatus;
import com.hotel.booking.enums.UserRole;

import com.hotel.booking.exception.BookingException;
import com.hotel.booking.exception.UnauthorizedException;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private HotelServiceClient hotelServiceClient;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @Mock
    private AuthorizationUtil authorizationUtil;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private UserContext guestContext;
    private RoomDto room;
    private HotelDto hotel;
    private BookingCreateRequest request;

    @BeforeEach
    void setup() {
        guestContext = UserContext.builder()
                .userId(1L)
                .username("guest")
                .role(UserRole.GUEST)
                .build();

        room = new RoomDto();
        room.setId(101L);
        room.setHotelId(1L);
        room.setRoomType("DELUXE");
        room.setRoomNumber("101");
        room.setIsActive(true);
        room.setPricePerNight(1000f);

        hotel = new HotelDto();
        hotel.setId(1L);
        hotel.setName("Test Hotel");

        request = BookingCreateRequest.builder()
                .hotelId(1L)
                .roomType("DELUXE")
                .checkInDate(LocalDate.now().plusDays(1))
                .checkOutDate(LocalDate.now().plusDays(3))
                .guestName("John")
                .guestEmail("john@test.com")
                .guestPhone("9999999999")
                .numberOfGuests(2)
                .build();
    }

    @Test
    void createBooking_success() {
        when(authorizationUtil.getUserContext()).thenReturn(guestContext);
        when(hotelServiceClient.getRoomsByHotelId(1L)).thenReturn(List.of(room));
        when(bookingRepository.findBookedRoomIds(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(hotelServiceClient.getRoomById(101L)).thenReturn(room);
        when(hotelServiceClient.getHotelByIdWrapped(1L))
                .thenReturn(ApiResponse.success(hotel, "ok"));
        when(bookingRepository.findConflictingBookingsWithLock(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(bookingRepository.save(any()))
                .thenAnswer(inv -> {
                    Booking b = inv.getArgument(0);
                    b.setId(10L);
                    return b;
                });

        BookingResponse response = bookingService.createBooking(request);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("DELUXE", response.getRoomType());
        verify(kafkaProducerService).publishBookingCreated(any());
    }

    @Test
    void createBooking_noAvailableRooms_throwsException() {
        when(authorizationUtil.getUserContext()).thenReturn(guestContext);
        when(hotelServiceClient.getRoomsByHotelId(1L)).thenReturn(List.of());

        assertThrows(BookingException.class,
                () -> bookingService.createBooking(request));
    }

    @Test
    void createBooking_conflictingBooking_throwsException() {
        when(authorizationUtil.getUserContext()).thenReturn(guestContext);
        when(hotelServiceClient.getRoomsByHotelId(1L)).thenReturn(List.of(room));
        when(bookingRepository.findBookedRoomIds(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(hotelServiceClient.getRoomById(101L)).thenReturn(room);
        when(hotelServiceClient.getHotelByIdWrapped(1L))
                .thenReturn(ApiResponse.success(hotel, "ok"));
        when(bookingRepository.findConflictingBookingsWithLock(any(), any(), any()))
                .thenReturn(List.of(new Booking()));

        assertThrows(BookingException.class,
                () -> bookingService.createBooking(request));
    }

//    @Test
//    void getBookingById_success() {
//        Booking booking = new Booking();
//        booking.setId(1L);
//        booking.setUserId(1L);
//        booking.setHotelId(1L);
//        booking.setRoomId(101L);
//
//        when(authorizationUtil.getUserContext()).thenReturn(guestContext);
//        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
//        when(hotelServiceClient.getRoomById(101L)).thenReturn(room);
//
//        BookingResponse response = bookingService.getBookingById(1L);
//        assertEquals(1L, response.getId());
//    }

//    @Test
//    void cancelBooking_success() {
//        Booking booking = new Booking();
//        booking.setId(1L);
//        booking.setUserId(1L);
//        booking.setHotelId(1L);
//        booking.setRoomId(101L);
//        booking.setStatus(BookingStatus.CONFIRMED);
//
//        when(authorizationUtil.getUserContext()).thenReturn(guestContext);
//        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
//        when(bookingRepository.save(any())).thenReturn(booking);
//        when(hotelServiceClient.getRoomById(101L)).thenReturn(room);
//
//        BookingResponse response = bookingService.cancelBooking(1L, "change");
//        assertEquals(BookingStatus.CANCELLED, response.getStatus());
//    }

    @Test
    void updatePaymentStatus_unauthorized() {
        UserContext guest = guestContext;
        when(authorizationUtil.getUserContext()).thenReturn(guest);

        Booking booking = new Booking();
        booking.setId(1L);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThrows(UnauthorizedException.class,
                () -> bookingService.updatePaymentStatus(1L, "PAID", "CASH"));
    }

    @Test
    void checkAvailability_success() {
        when(hotelServiceClient.getRoomsByHotelId(1L))
                .thenReturn(List.of(room));
        when(bookingRepository.findBookedRoomIds(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        AvailabilityResponse response =
                bookingService.checkAvailability(1L,
                        LocalDate.now().plusDays(1),
                        LocalDate.now().plusDays(2));

        assertEquals(1, response.getAvailableRooms());
    }

    @Test
    void getAvailableRoomTypes_success() {
        when(hotelServiceClient.getRoomsByHotelId(1L))
                .thenReturn(List.of(room));
        when(bookingRepository.findBookedRoomIds(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        List<AvailableRoomTypeDto> result =
                bookingService.getAvailableRoomTypes(
                        1L,
                        LocalDate.now().plusDays(1),
                        LocalDate.now().plusDays(2));

        assertEquals(1, result.size());
        assertEquals("DELUXE", result.get(0).getRoomType());
    }
}
