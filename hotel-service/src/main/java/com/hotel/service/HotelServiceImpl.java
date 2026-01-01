// HotelServiceImpl.java (Implementation)
package com.hotel.service;

import com.hotel.dto.request.HotelRequestDto;
import com.hotel.dto.response.HotelResponseDto;
import com.hotel.entity.Hotel;
import com.hotel.enums.HotelStatus;
import com.hotel.enums.RoomStatus;
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

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HotelServiceImpl implements HotelService {

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public HotelResponseDto createHotel(HotelRequestDto requestDto, Long userId) {
        log.info("Creating hotel: {} by user: {}", requestDto.getName(), userId);
        Hotel hotel = modelMapper.map(requestDto, Hotel.class);
        if (hotel.getStatus() == null) {
            hotel.setStatus(HotelStatus.ACTIVE);
        }
        Hotel savedHotel = hotelRepository.save(hotel);
        log.info("Hotel created successfully with ID: {}", savedHotel.getId());
        return modelMapper.map(savedHotel, HotelResponseDto.class);
    }

    @Override
    @Transactional
    public HotelResponseDto updateHotel(Long hotelId, HotelRequestDto requestDto,
                                        Long userId, String role, Long userHotelId) {
        log.info("Updating hotel: {} by user: {} with role: {}", hotelId, userId, role);
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", "id", hotelId));
        // Authorization check
        if (UserRole.MANAGER.name().equals(role) || UserRole.RECEPTIONIST.name().equals(role)) {
            if (userHotelId == null || !userHotelId.equals(hotelId)) {
                throw new ForbiddenException("You can only update your own hotel");
            }
        }
        // Update fields
        hotel.setName(requestDto.getName());
        hotel.setDescription(requestDto.getDescription());
        hotel.setAddress(requestDto.getAddress());
        hotel.setCity(requestDto.getCity());
        hotel.setState(requestDto.getState());
        hotel.setCountry(requestDto.getCountry());
        hotel.setPincode(requestDto.getPincode());
        hotel.setContactNumber(requestDto.getContactNumber());
        hotel.setEmail(requestDto.getEmail());
        hotel.setStarRating(requestDto.getStarRating());
        hotel.setAmenities(requestDto.getAmenities());
        if (requestDto.getStatus() != null) {
            hotel.setStatus(requestDto.getStatus());
        }
        Hotel updatedHotel = hotelRepository.save(hotel);
        log.info("Hotel updated successfully: {}", hotelId);
        return modelMapper.map(updatedHotel, HotelResponseDto.class);
    }

    @Override
    public HotelResponseDto getHotelById(Long hotelId) {
        log.info("Fetching hotel by ID: {}", hotelId);
        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", "id", hotelId));
        return modelMapper.map(hotel, HotelResponseDto.class);
    }

    @Override
    public List<HotelResponseDto> getAllHotels() {
        log.info("Fetching all hotels");
        List<Hotel> hotels = hotelRepository.findAll();
        return hotels.stream()
                .map(hotel -> modelMapper.map(hotel, HotelResponseDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<HotelResponseDto> getActiveHotels() {
        log.info("Fetching active hotels");

        List<Hotel> hotels = hotelRepository.findByStatusOrderByNameAsc(HotelStatus.ACTIVE);
        return hotels.stream()
                .map(hotel -> modelMapper.map(hotel, HotelResponseDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<HotelResponseDto> searchHotels(String city) {
        log.info("Searching hotels with city: {} and minRating: {}", city);

        List<Hotel> hotels = hotelRepository.searchHotels(HotelStatus.ACTIVE, city);
        return hotels.stream()
                .map(hotel -> modelMapper.map(hotel, HotelResponseDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public HotelResponseDto getMyHotel(Long hotelId) {
        log.info("Fetching hotel for manager/receptionist: {}", hotelId);

        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", "id", hotelId));

        return modelMapper.map(hotel, HotelResponseDto.class);
    }

    @Override
    @Transactional
    public void deleteHotel(Long hotelId, Long userId, String role) {
        log.info("Deleting hotel: {} by user: {} with role: {}", hotelId, userId, role);

        // Only ADMIN can delete hotels
        if (!UserRole.ADMIN.name().equals(role)) {   // --TODO change ,edit userRoles to "Admin".equals(role)) later
            throw new ForbiddenException("Only administrators can delete hotels");
        }

        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", "id", hotelId));

        // Soft delete - change status to INACTIVE
        hotel.setStatus(HotelStatus.INACTIVE);
        hotelRepository.save(hotel);

        log.info("Hotel soft deleted successfully: {}", hotelId);
    }

    @Override
    @Transactional
    public void updateHotelRoomCounts(Long hotelId) {
        log.info("Updating room counts for hotel: {}", hotelId);

        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel", "id", hotelId));

        long totalRooms = roomRepository.countByHotelId(hotelId);
        long availableRooms = roomRepository.countByHotelIdAndStatus(hotelId, RoomStatus.AVAILABLE);

        hotel.setTotalRooms((int) totalRooms);
        hotel.setAvailableRooms((int) availableRooms);
        hotelRepository.save(hotel);

        log.info("Room counts updated - Total: {}, Available: {}", totalRooms, availableRooms);
    }
}