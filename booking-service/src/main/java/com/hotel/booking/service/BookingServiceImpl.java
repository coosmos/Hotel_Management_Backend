package com.hotel.booking.service;

import com.hotel.booking.client.HotelServiceClient;
import com.hotel.booking.dto.external.RoomDto;
import com.hotel.booking.dto.request.BookingCreateRequest;
import com.hotel.booking.dto.request.CheckInRequest;
import com.hotel.booking.dto.request.CheckOutRequest;
import com.hotel.booking.dto.response.AvailabilityResponse;
import com.hotel.booking.dto.response.BookingResponse;
import com.hotel.booking.entity.Booking;
import com.hotel.booking.enums.BookingStatus;
import com.hotel.booking.event.BookingCreatedEvent;
import com.hotel.booking.event.GuestCheckedInEvent;
import com.hotel.booking.event.GuestCheckedOutEvent;
import com.hotel.booking.exception.BookingException;
import com.hotel.booking.exception.ResourceNotFoundException;
import com.hotel.booking.exception.UnauthorizedException;
import com.hotel.booking.repository.BookingRepository;
import com.hotel.booking.security.AuthorizationUtil;
import com.hotel.booking.security.UserContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final HotelServiceClient hotelServiceClient;
    private final KafkaProducerService kafkaProducerService;
    private final AuthorizationUtil authorizationUtil;

    @Override
    @Transactional(readOnly = true)
    public AvailabilityResponse checkAvailability(Long hotelId, LocalDate checkInDate, LocalDate checkOutDate) {
        log.info("Checking availability for hotel {} from {} to {}", hotelId, checkInDate, checkOutDate);
        validateDates(checkInDate, checkOutDate);
        List<RoomDto> allRooms = hotelServiceClient.getRoomsByHotelId(hotelId);
        log.debug("Found {} total rooms for hotel {}", allRooms.size(), hotelId);
        List<Long> bookedRoomIds = bookingRepository.findBookedRoomIds(hotelId, checkInDate, checkOutDate);
        log.debug("Found {} booked rooms for the date range", bookedRoomIds.size());
        List<RoomDto> availableRooms = allRooms.stream()
                .filter(room -> room.getIsActive())
                .filter(room -> "AVAILABLE".equalsIgnoreCase(room.getStatus()))
                .filter(room -> !bookedRoomIds.contains(room.getId()))
                .collect(Collectors.toList());
        log.info("Found {} available rooms for hotel {} in date range", availableRooms.size(), hotelId);
        return AvailabilityResponse.builder()
                .hotelId(hotelId)
                .checkInDate(checkInDate)
                .checkOutDate(checkOutDate)
                .totalRooms(allRooms.size())
                .availableRooms(availableRooms.size())
                .availableRoomList(availableRooms)
                .build();
    }

    @Override
    @Transactional
    public BookingResponse createBooking(BookingCreateRequest request) {
        UserContext context = authorizationUtil.getUserContext();
        log.info("Creating booking for user {} in hotel {}", context.getUserId(), request.getHotelId());
        // Only guest can create bookings
        if (!context.isGuest() && !context.isAdmin()) {
            throw new UnauthorizedException("Only guests can create bookings");
        }
        // Validate request
        if (!request.isValidDateRange()) {
            throw new BookingException("Check-out date must be after check-in date");
        }
        validateDates(request.getCheckInDate(), request.getCheckOutDate());
        // Check if room exists and is available
        RoomDto room = hotelServiceClient.getRoomById(request.getRoomId());
        if (!room.getHotelId().equals(request.getHotelId())) {
            throw new BookingException("Room does not belong to specified hotel");
        }
        if (!room.getIsActive() || !"AVAILABLE".equalsIgnoreCase(room.getStatus())) {
            throw new BookingException("Room is not available for booking");
        }
        // This query locks matching rows in the database
        // Other transactions trying to book the same room will wait here
        log.debug("Acquiring lock and checking for conflicting bookings for room {}", request.getRoomId());
        List<Booking> conflicts = bookingRepository.findConflictingBookingsWithLock(
                request.getRoomId(),
                request.getCheckInDate(),
                request.getCheckOutDate()
        );
        if (!conflicts.isEmpty()) {
            log.warn("Room {} is already booked for dates {} to {}",
                    request.getRoomId(), request.getCheckInDate(), request.getCheckOutDate());
            throw new BookingException("Room is already booked for the selected dates");
        }
        log.debug("No conflicts found, proceeding with booking creation");
        // Calculate total amount
        int numberOfNights = request.getNumberOfNights();
        float totalAmount = room.getPricePerNight() * numberOfNights;
        // Create booking entity
        Booking booking = Booking.builder()
                .userId(context.getUserId())
                .hotelId(request.getHotelId())
                .roomId(request.getRoomId())
                .checkInDate(request.getCheckInDate())
                .checkOutDate(request.getCheckOutDate())
                .totalAmount(totalAmount)
                .status(BookingStatus.CONFIRMED)
                .guestName(request.getGuestName())
                .guestEmail(request.getGuestEmail())
                .guestPhone(request.getGuestPhone())
                .numberOfGuests(request.getNumberOfGuests())
                .build();
        booking.setCreatedBy(context.getUsername());
        Booking savedBooking = bookingRepository.save(booking);
        log.info("Booking created with ID: {}", savedBooking.getId());
        publishBookingCreatedEvent(savedBooking, room);
        return mapToResponse(savedBooking, room); //lock is released on transaction complete
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long bookingId) {
        Booking booking = findBookingById(bookingId);
        authorizationUtil.verifyBookingAccess(booking.getUserId(), booking.getHotelId());
        RoomDto room = hotelServiceClient.getRoomById(booking.getRoomId());
        return mapToResponse(booking, room);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings() {
        UserContext context = authorizationUtil.getUserContext();
        if (!context.isGuest()) {
            throw new UnauthorizedException("Only guests can view their bookings");
        }
        List<Booking> bookings = bookingRepository.findByUserIdOrderByCreatedAtDesc(context.getUserId());
        return bookings.stream()
                .map(this::mapToResponseWithRoom)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getHotelBookings(Long hotelId) {
        authorizationUtil.verifyHotelAccess(hotelId);
        List<Booking> bookings = bookingRepository.findByHotelIdOrderByCreatedAtDesc(hotelId);
        return bookings.stream()
                .map(this::mapToResponseWithRoom)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getAllBookings() {
        UserContext context = authorizationUtil.getUserContext();
        if (!context.isAdmin()) {
            throw new UnauthorizedException("Only admins can view all bookings");
        }
        List<Booking> bookings = bookingRepository.findAll();
        return bookings.stream()
                .map(this::mapToResponseWithRoom)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(Long bookingId, String reason) {
        Booking booking = findBookingById(bookingId);
        UserContext context = authorizationUtil.getUserContext();

        if (context.isGuest() && !booking.getUserId().equals(context.getUserId())) {
            throw new UnauthorizedException("You can only cancel your own bookings");
        } else if (context.isStaff()) {
            authorizationUtil.verifyHotelAccess(booking.getHotelId());
        }

        if (!booking.getStatus().isCancellable()) {
            throw new BookingException(
                    "Booking cannot be cancelled. Current status: " + booking.getStatus().getDisplayName());
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDate.now());
        booking.setUpdatedBy(context.getUsername());
        Booking updatedBooking = bookingRepository.save(booking);
        log.info("Booking {} cancelled by user {}", bookingId, context.getUserId());
        RoomDto room = hotelServiceClient.getRoomById(booking.getRoomId());
        return mapToResponse(updatedBooking, room);
    }

    @Override
    @Transactional
    public BookingResponse checkInGuest(Long bookingId, CheckInRequest request) {
        Booking booking = findBookingById(bookingId);
        UserContext context = authorizationUtil.getUserContext();
        if (!context.canManageBookings()) {
            throw new UnauthorizedException("Only staff can check in guests");
        }
        authorizationUtil.verifyHotelAccess(booking.getHotelId());

        if (!booking.getStatus().canCheckIn()) {
            throw new BookingException(
                    "Cannot check in. Current status: " + booking.getStatus().getDisplayName());
        }
        booking.setStatus(BookingStatus.CHECKED_IN);
        booking.setCheckedInAt(LocalDate.now());
        booking.setUpdatedBy(context.getUsername());

        Booking updatedBooking = bookingRepository.save(booking);
        log.info("Guest checked in for booking {}", bookingId);
        try {
            hotelServiceClient.updateRoomStatus(booking.getRoomId(), "OCCUPIED");
        } catch (Exception e) {
            log.error("Failed to update room status: {}", e.getMessage());
        }
        publishGuestCheckedInEvent(updatedBooking);
        RoomDto room = hotelServiceClient.getRoomById(booking.getRoomId());
        return mapToResponse(updatedBooking, room);
    }
    @Override
    @Transactional
    public BookingResponse checkOutGuest(Long bookingId, CheckOutRequest request) {
        Booking booking = findBookingById(bookingId);
        UserContext context = authorizationUtil.getUserContext();

        if (!context.canManageBookings()) {
            throw new UnauthorizedException("Only staff can check out guests");
        }
        authorizationUtil.verifyHotelAccess(booking.getHotelId());

        if (!booking.getStatus().canCheckOut()) {
            throw new BookingException("Cannot check out. Current status: " + booking.getStatus().getDisplayName());
        }
        booking.setStatus(BookingStatus.CHECKED_OUT);
        booking.setCheckedOutAt(LocalDate.now());
        booking.setUpdatedBy(context.getUsername());
        Booking updatedBooking = bookingRepository.save(booking);
        log.info("Guest checked out for booking {}", bookingId);
        try {
            hotelServiceClient.updateRoomStatus(booking.getRoomId(), "CLEANING");
        } catch (Exception e) {
            log.error("Failed to update room status: {}", e.getMessage());
        }
        publishGuestCheckedOutEvent(updatedBooking, request);
        RoomDto room = hotelServiceClient.getRoomById(booking.getRoomId());
        return mapToResponse(updatedBooking, room);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getTodayCheckIns(Long hotelId) {
        authorizationUtil.verifyHotelAccess(hotelId);
        List<Booking> bookings = bookingRepository.findUpcomingCheckIns(hotelId, LocalDate.now());
        return bookings.stream()
                .map(this::mapToResponseWithRoom)
                .collect(Collectors.toList());
    }
    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getTodayCheckOuts(Long hotelId) {
        authorizationUtil.verifyHotelAccess(hotelId);
        List<Booking> bookings = bookingRepository.findUpcomingCheckOuts(hotelId, LocalDate.now());
        return bookings.stream()
                .map(this::mapToResponseWithRoom)
                .collect(Collectors.toList());
    }
    // Helper methods
    private Booking findBookingById(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
    }

    private void validateDates(LocalDate checkInDate, LocalDate checkOutDate) {
        LocalDate today = LocalDate.now();
        if (checkInDate.isBefore(today)) {
            throw new BookingException("Check-in date cannot be in the past");
        }
        if (checkOutDate.isBefore(checkInDate) || checkOutDate.isEqual(checkInDate)) {
            throw new BookingException("Check-out date must be after check-in date");
        }
    }
    private BookingResponse mapToResponse(Booking b, RoomDto room) {
        return BookingResponse.builder()
                .id(b.getId())
                .userId(b.getUserId())
                .hotelId(b.getHotelId())
                .roomId(b.getRoomId())
                .roomNumber(room.getRoomNumber())
                .roomType(room.getRoomType())
                .checkInDate(b.getCheckInDate())
                .checkOutDate(b.getCheckOutDate())
                .totalAmount(b.getTotalAmount())
                .status(b.getStatus())
                .guestName(b.getGuestName())
                .guestEmail(b.getGuestEmail())
                .guestPhone(b.getGuestPhone())
                .numberOfGuests(b.getNumberOfGuests())
                .numberOfNights(b.getNumberOfNights())
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .build();
    }
    private BookingResponse mapToResponseWithRoom(Booking booking) {
        try {
            return mapToResponse(booking,
                    hotelServiceClient.getRoomById(booking.getRoomId()));
        } catch (Exception e) {
            return mapToResponse(booking, new RoomDto());
        }
    }
    private void publishBookingCreatedEvent(Booking booking, RoomDto room) {
        BookingCreatedEvent event = BookingCreatedEvent.builder()
                .bookingId(booking.getId())
                .userId(booking.getUserId())
                .hotelId(booking.getHotelId())
                .roomId(booking.getRoomId())
                .guestName(booking.getGuestName())
                .guestEmail(booking.getGuestEmail())
                .guestPhone(booking.getGuestPhone())
                .checkInDate(booking.getCheckInDate())
                .checkOutDate(booking.getCheckOutDate())
                .totalAmount(booking.getTotalAmount())
                .numberOfGuests(booking.getNumberOfGuests())
                .createdAt(LocalDateTime.now())
                .build();
        kafkaProducerService.publishBookingCreated(event);
    }
    private void publishGuestCheckedInEvent(Booking booking) {
        GuestCheckedInEvent event = GuestCheckedInEvent.builder()
                .bookingId(booking.getId())
                .userId(booking.getUserId())
                .hotelId(booking.getHotelId())
                .roomId(booking.getRoomId())
                .guestName(booking.getGuestName())
                .guestEmail(booking.getGuestEmail())
                .checkInDate(booking.getCheckInDate())
                .checkOutDate(booking.getCheckOutDate())
                .checkedInAt(LocalDateTime.now())
                .roomStatus("OCCUPIED")
                .build();
        kafkaProducerService.publishGuestCheckedIn(event);
    }
    private void publishGuestCheckedOutEvent(Booking booking, CheckOutRequest request) {
        GuestCheckedOutEvent event = GuestCheckedOutEvent.builder()
                .bookingId(booking.getId())
                .userId(booking.getUserId())
                .hotelId(booking.getHotelId())
                .roomId(booking.getRoomId())
                .guestName(booking.getGuestName())
                .guestEmail(booking.getGuestEmail())
                .checkInDate(booking.getCheckInDate())
                .checkOutDate(booking.getCheckOutDate())
                .checkedOutAt(LocalDateTime.now())
                .roomStatus("CLEANING")
                .rating(request.getRating())
                .feedback(request.getFeedback())
                .build();
        kafkaProducerService.publishGuestCheckedOut(event);
    }
}