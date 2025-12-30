package com.hotel.service;

import com.hotel.dto.request.RoomRequestDto;
import com.hotel.dto.request.UpdateRoomStatusDto;
import com.hotel.dto.response.RoomResponseDto;
import com.hotel.entity.Hotel;
import com.hotel.entity.Room;
import com.hotel.enums.RoomStatus;
import com.hotel.enums.RoomType;
import com.hotel.enums.UserRole;
import com.hotel.exception.BadRequestException;
import com.hotel.exception.ForbiddenException;
import com.hotel.exception.ResourceNotFoundException;
import com.hotel.repository.HotelRepository;
import com.hotel.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final HotelService hotelService;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public RoomResponseDto createRoom(RoomRequestDto requestDto, Long userId,
                                      String role, Long userHotelId) {
        log.info("Creating room for hotel: {} by user: {}", requestDto.getHotelId(), userId);

        // Authorization check
        validateHotelAccess(requestDto.getHotelId(), role, userHotelId, "create rooms for");

        // Check if hotel exists
        Hotel hotel = hotelRepository.findById(requestDto.getHotelId())
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", "id", requestDto.getHotelId()));

        // Check if room number already exists for this hotel
        if (roomRepository.existsByHotelIdAndRoomNumber(requestDto.getHotelId(), requestDto.getRoomNumber())) {
            throw new BadRequestException("Room number " + requestDto.getRoomNumber() +
                    " already exists for this hotel");
        }

        Room room = modelMapper.map(requestDto, Room.class);
        room.setHotel(hotel);
        room.setCreatedBy(userId);
        room.setUpdatedBy(userId);

        if (room.getStatus() == null) {
            room.setStatus(RoomStatus.AVAILABLE);
        }
        if (room.getIsActive() == null) {
            room.setIsActive(true);
        }

        Room savedRoom = roomRepository.save(room);

        // Update hotel room counts
        hotelService.updateHotelRoomCounts(requestDto.getHotelId());

        log.info("Room created successfully with ID: {}", savedRoom.getId());

        return convertToResponseDto(savedRoom);
    }

    @Override
    @Transactional
    public RoomResponseDto updateRoom(Long roomId, RoomRequestDto requestDto,
                                      Long userId, String role, Long userHotelId) {
        log.info("Updating room: {} by user: {}", roomId, userId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", roomId));

        // Authorization check
        validateHotelAccess(room.getHotel().getId(), role, userHotelId, "update rooms for");

        // Update fields
        room.setRoomNumber(requestDto.getRoomNumber());
        room.setRoomType(requestDto.getRoomType());
        room.setPricePerNight(requestDto.getPricePerNight());
        room.setMaxOccupancy(requestDto.getMaxOccupancy());
        room.setFloorNumber(requestDto.getFloorNumber());
        room.setBedType(requestDto.getBedType());
        room.setRoomSize(requestDto.getRoomSize());
        room.setAmenities(requestDto.getAmenities());
        room.setDescription(requestDto.getDescription());

        if (requestDto.getStatus() != null) {
            room.updateStatus(requestDto.getStatus(), userId);
        }
        if (requestDto.getIsActive() != null) {
            room.setIsActive(requestDto.getIsActive());
        }

        room.setUpdatedBy(userId);

        Room updatedRoom = roomRepository.save(room);

        // Update hotel room counts
        hotelService.updateHotelRoomCounts(room.getHotel().getId());

        log.info("Room updated successfully: {}", roomId);

        return convertToResponseDto(updatedRoom);
    }

    @Override
    @Transactional
    public RoomResponseDto updateRoomStatus(Long roomId, UpdateRoomStatusDto statusDto,
                                            Long userId, String role, Long userHotelId) {
        log.info("Updating room status: {} to {} by user: {}", roomId, statusDto.getStatus(), userId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", roomId));

        // Authorization check - MANAGER and RECEPTIONIST can update status
        validateHotelAccess(room.getHotel().getId(), role, userHotelId, "update room status for");

        room.updateStatus(statusDto.getStatus(), userId);
        Room updatedRoom = roomRepository.save(room);

        // Update hotel room counts
        hotelService.updateHotelRoomCounts(room.getHotel().getId());

        log.info("Room status updated successfully: {} to {}", roomId, statusDto.getStatus());

        return convertToResponseDto(updatedRoom);
    }

    @Override
    public RoomResponseDto getRoomById(Long roomId) {
        log.info("Fetching room by ID: {}", roomId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", roomId));

        return convertToResponseDto(room);
    }

    @Override
    public List<RoomResponseDto> getRoomsByHotelId(Long hotelId) {
        log.info("Fetching all rooms for hotel: {}", hotelId);

        List<Room> rooms = roomRepository.findByHotelId(hotelId);
        return rooms.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<RoomResponseDto> getAvailableRoomsByHotelId(Long hotelId) {
        log.info("Fetching available rooms for hotel: {}", hotelId);

        List<Room> rooms = roomRepository.findByHotelIdAndStatusAndIsActive(
                hotelId, RoomStatus.AVAILABLE, true);
        return rooms.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<RoomResponseDto> searchRooms(Long hotelId, RoomStatus status,
                                             RoomType roomType, BigDecimal minPrice,
                                             BigDecimal maxPrice) {
        log.info("Searching rooms for hotel: {} with filters", hotelId);

        List<Room> rooms = roomRepository.searchRooms(hotelId, status, roomType, minPrice, maxPrice);
        return rooms.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteRoom(Long roomId, Long userId, String role, Long userHotelId) {
        log.info("Deleting room: {} by user: {}", roomId, userId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", roomId));

        // Authorization check - Only ADMIN and MANAGER can delete rooms
        if (UserRole.RECEPTIONIST.name().equals(role)) {
            throw new ForbiddenException("Receptionists cannot delete rooms");
        }
        validateHotelAccess(room.getHotel().getId(), role, userHotelId, "delete rooms for");

        Long hotelId = room.getHotel().getId();
        roomRepository.delete(room);

        // Update hotel room counts
        hotelService.updateHotelRoomCounts(hotelId);

        log.info("Room deleted successfully: {}", roomId);
    }

    // Helper method to validate hotel access
    private void validateHotelAccess(Long hotelId, String role, Long userHotelId, String action) {
        if (UserRole.ADMIN.name().equals(role)) {
            return; // ADMIN has access to all hotels
        }

        if (UserRole.MANAGER.name().equals(role) || UserRole.RECEPTIONIST.name().equals(role)) {
            if (userHotelId == null || !userHotelId.equals(hotelId)) {
                throw new ForbiddenException("You can only " + action + " your own hotel");
            }
        } else if (UserRole.GUEST.name().equals(role)) {
            throw new ForbiddenException("Guests cannot " + action + " hotels");
        }
    }

    // Helper method to convert Room to RoomResponseDto
    private RoomResponseDto convertToResponseDto(Room room) {
        RoomResponseDto dto = modelMapper.map(room, RoomResponseDto.class);
        dto.setHotelId(room.getHotel().getId());
        dto.setHotelName(room.getHotel().getName());
        return dto;
    }
}