package com.hotel.booking.service;

import com.hotel.booking.client.HotelServiceClient;
import com.hotel.booking.dto.external.HotelDto;
import com.hotel.booking.dto.external.RoomDto;
import com.hotel.booking.dto.request.BookingCreateRequest;
import com.hotel.booking.dto.request.CheckInRequest;
import com.hotel.booking.dto.request.CheckOutRequest;
import com.hotel.booking.dto.response.*;
import com.hotel.booking.entity.Booking;
import com.hotel.booking.enums.BookingStatus;
import com.hotel.booking.enums.PaymentStatus;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
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
    @Transactional(readOnly = true)
    public List<AvailableHotelDto> searchAvailableHotels(String city, LocalDate checkInDate, LocalDate checkOutDate) {
        log.info("Searching available hotels in {} from {} to {}", city, checkInDate, checkOutDate);
        validateDates(checkInDate, checkOutDate);

        // get all hotels in city from hotel-service
        ApiResponse<List<HotelDto>> response = hotelServiceClient.searchHotelsWrapped(city);
        List<HotelDto> allHotels = response.getData();
        log.debug("Found {} hotels in {}", allHotels.size(), city);

        List<AvailableHotelDto> availableHotels = new ArrayList<>();

        for (HotelDto hotel : allHotels) {
            // get all rooms for this hotel
            List<RoomDto> allRooms = hotelServiceClient.getRoomsByHotelId(hotel.getId());

            // get booked room ids for date range
            List<Long> bookedRoomIds = bookingRepository.findBookedRoomIds(
                    hotel.getId(), checkInDate, checkOutDate
            );

            // calculate available rooms
            long availableCount = allRooms.stream()
                    .filter(room -> room.getIsActive())
                    .filter(room -> !bookedRoomIds.contains(room.getId()))
                    .count();

            // only include hotels with available rooms
            if (availableCount > 0) {
                AvailableHotelDto availableHotel = new AvailableHotelDto();
                availableHotel.setHotelId(hotel.getId());
                availableHotel.setHotelName(hotel.getName());
                availableHotel.setDescription(hotel.getDescription());
                availableHotel.setAddress(hotel.getAddress());
                availableHotel.setCity(hotel.getCity());
                availableHotel.setState(hotel.getState());
                availableHotel.setCountry(hotel.getCountry());
                availableHotel.setPincode(hotel.getPincode());
                availableHotel.setPhoneNumber(hotel.getPhoneNumber());
                availableHotel.setEmail(hotel.getEmail());
                availableHotel.setTotalRooms(hotel.getTotalRooms());
                availableHotel.setAvailableRoomsCount((int) availableCount);
                availableHotel.setStatus(hotel.getStatus());
                availableHotel.setAmenities(hotel.getAmenities());

                availableHotels.add(availableHotel);
            }
        }

        log.info("Found {} hotels with availability in {}", availableHotels.size(), city);
        return availableHotels;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailableRoomTypeDto> getAvailableRoomTypes(Long hotelId, LocalDate checkInDate, LocalDate checkOutDate) {
        log.info("Getting available room types for hotel {} from {} to {}", hotelId, checkInDate, checkOutDate);
        validateDates(checkInDate, checkOutDate);

        // get all rooms for hotel
        List<RoomDto> allRooms = hotelServiceClient.getRoomsByHotelId(hotelId);

        // get booked room ids
        List<Long> bookedRoomIds = bookingRepository.findBookedRoomIds(hotelId, checkInDate, checkOutDate);
        // filter available rooms
        List<RoomDto> availableRooms = allRooms.stream()
                .filter(room -> room.getIsActive())
                .filter(room -> !bookedRoomIds.contains(room.getId()))
                .collect(Collectors.toList());

        // group by room type and create DTOs
        Map<String, List<RoomDto>> roomsByType = availableRooms.stream()
                .collect(Collectors.groupingBy(RoomDto::getRoomType));

        List<AvailableRoomTypeDto> roomTypes = new ArrayList<>();

        for (Map.Entry<String, List<RoomDto>> entry : roomsByType.entrySet()) {
            List<RoomDto> rooms = entry.getValue();
            if (!rooms.isEmpty()) {
                RoomDto firstRoom = rooms.get(0); // get first room for type details

                AvailableRoomTypeDto dto = new AvailableRoomTypeDto();
                dto.setRoomType(entry.getKey());
                dto.setPricePerNight(BigDecimal.valueOf(firstRoom.getPricePerNight()));
                dto.setAvailableCount(rooms.size());
                dto.setMaxOccupancy(firstRoom.getMaxOccupancy());
                dto.setDescription(firstRoom.getDescription());
                dto.setAmenities(firstRoom.getAmenities());
                dto.setBedType(firstRoom.getBedType());

                roomTypes.add(dto);
            }
        }

        log.info("Found {} room types available for hotel {}", roomTypes.size(), hotelId);
        return roomTypes;
    }

    @Override
    @Transactional
    public BookingResponse createBooking(BookingCreateRequest request) {
        UserContext context = authorizationUtil.getUserContext();
        log.info("Creating booking for user {} in hotel {} for room type {}",
                context.getUserId(), request.getHotelId(), request.getRoomType());

        // only guest can create bookings
        if (!context.isGuest() && !context.isAdmin()) {
            throw new UnauthorizedException("Only guests can create bookings");
        }

        // validate dates
        validateDates(request.getCheckInDate(), request.getCheckOutDate());

        // find available room of requested type
        Long assignedRoomId = findAvailableRoomByType(
                request.getHotelId(),
                request.getRoomType(),
                request.getCheckInDate(),
                request.getCheckOutDate()
        );

        if (assignedRoomId == null) {
            throw new BookingException("No rooms of type " + request.getRoomType() + " available for selected dates");
        }

        // get room details
        RoomDto room = hotelServiceClient.getRoomById(assignedRoomId);
        ApiResponse hotelDtoApiResponse=hotelServiceClient.getHotelByIdWrapped(request.getHotelId());
       HotelDto hotel=(HotelDto) hotelDtoApiResponse.getData();
        // double-check room belongs to hotel
        if (!room.getHotelId().equals(request.getHotelId())) {
            throw new BookingException("Room does not belong to specified hotel");
        }

        // pessimistic lock check for race conditions
        log.debug("Acquiring lock and checking for conflicting bookings for room {}", assignedRoomId);
        List<Booking> conflicts = bookingRepository.findConflictingBookingsWithLock(
                assignedRoomId,
                request.getCheckInDate(),
                request.getCheckOutDate()
        );

        if (!conflicts.isEmpty()) {
            log.warn("Room {} was just booked by another user, trying to find another room", assignedRoomId);
            throw new BookingException("Room was just booked. Please try again.");
        }

        log.debug("No conflicts found, proceeding with booking creation for room {}", assignedRoomId);

        // calculate total amount
        int numberOfNights = (int) (request.getCheckOutDate().toEpochDay() - request.getCheckInDate().toEpochDay());
        float totalAmount = room.getPricePerNight() * numberOfNights;

        // create booking entity
        Booking booking = Booking.builder()
                .userId(context.getUserId())
                .hotelId(request.getHotelId())
                .roomId(assignedRoomId) // backend assigned room
                .checkInDate(request.getCheckInDate())
                .checkOutDate(request.getCheckOutDate())
                .totalAmount(totalAmount)
                .paymentStatus(PaymentStatus.PENDING)
                .status(BookingStatus.CONFIRMED)
                .guestName(request.getGuestName())
                .guestEmail(request.getGuestEmail())
                .guestPhone(request.getGuestPhone())
                .numberOfGuests(request.getNumberOfGuests())
                .hotelName(hotel.getName())
                .build();

        booking.setCreatedBy(context.getUsername());
        Booking savedBooking = bookingRepository.save(booking);

        log.info("Booking created with ID: {} for room {}", savedBooking.getId(), assignedRoomId);

        publishBookingCreatedEvent(savedBooking, room);

        return mapToResponse(savedBooking, room);
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
        if(booking.getPaymentStatus()==PaymentStatus.PENDING){
            booking.setPaymentStatus(PaymentStatus.PAID);
            booking.setPaymentMethod("CASH");
            booking.setPaidAt(LocalDateTime.now());
        }
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

    // helper methods
     //* find an available room of specified type for the date range
    private Long findAvailableRoomByType(Long hotelId, String roomType, LocalDate checkIn, LocalDate checkOut) {
        log.debug("Finding available room of type {} for hotel {}", roomType, hotelId);

        // get all rooms of this type
        List<RoomDto> allRooms = hotelServiceClient.getRoomsByHotelId(hotelId);
        List<Long> roomsOfType = allRooms.stream()
                .filter(r -> r.getRoomType().equalsIgnoreCase(roomType))
                .filter(r -> r.getIsActive())
                .map(RoomDto::getId)
                .collect(Collectors.toList());

        if (roomsOfType.isEmpty()) {
            log.warn("No rooms of type {} found in hotel {}", roomType, hotelId);
            return null;
        }

        // get booked rooms
        List<Long> bookedRoomIds = bookingRepository.findBookedRoomIds(hotelId, checkIn, checkOut);

        // find first available room of this type
        Optional<Long> availableRoom = roomsOfType.stream()
                .filter(roomId -> !bookedRoomIds.contains(roomId))
                .findFirst();

        if (availableRoom.isPresent()) {
            log.debug("Found available room {} of type {}", availableRoom.get(), roomType);
        } else {
            log.warn("No available rooms of type {} for dates {} to {}", roomType, checkIn, checkOut);
        }

        return availableRoom.orElse(null);
    }

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
                .hotelName(b.getHotelName())
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