package com.hotel.booking.service;

import com.hotel.booking.client.HotelServiceClient;
import com.hotel.booking.dto.analytics.DashboardAnalyticsDto;
import com.hotel.booking.dto.analytics.HotelAnalyticsDto;
import com.hotel.booking.dto.analytics.RevenueByDateDto;
import com.hotel.booking.dto.analytics.RoomTypeAnalyticsDto;
import com.hotel.booking.dto.external.HotelDto;
import com.hotel.booking.dto.external.RoomDto;
import com.hotel.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final BookingRepository bookingRepository;
    private final HotelServiceClient hotelServiceClient;

    @Override
    @Transactional(readOnly = true)
    public DashboardAnalyticsDto getDashboardAnalytics() {
        log.info("Fetching dashboard analytics");

        Long totalBookings = bookingRepository.getTotalBookings();
        Double totalRevenue = bookingRepository.getTotalRevenue();
        Long activeBookings = bookingRepository.getActiveBookings();
        Long completedBookings = bookingRepository.getCompletedBookings();
        Long cancelledBookings = bookingRepository.getCancelledBookings();
        Long todayCheckIns = bookingRepository.getTodayCheckInsCount(LocalDate.now());
        Long todayCheckOuts = bookingRepository.getTodayCheckOutsCount(LocalDate.now());
        Long pendingPayments = bookingRepository.getPendingPayments();
        Double averageBookingValue = bookingRepository.getAverageBookingValue();

        return DashboardAnalyticsDto.builder()
                .totalBookings(totalBookings)
                .totalRevenue(totalRevenue != null ? totalRevenue : 0.0)
                .activeBookings(activeBookings)
                .completedBookings(completedBookings)
                .cancelledBookings(cancelledBookings)
                .todayCheckIns(todayCheckIns)
                .todayCheckOuts(todayCheckOuts)
                .pendingPayments(pendingPayments)
                .averageBookingValue(averageBookingValue != null ? averageBookingValue : 0.0)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HotelAnalyticsDto> getHotelAnalytics() {
        log.info("Fetching hotel analytics");

        List<HotelAnalyticsDto> analytics = bookingRepository.getRevenueByHotel();

        // enrich with hotel names from hotel-service
        for (HotelAnalyticsDto dto : analytics) {
            try {
                HotelDto hotel = hotelServiceClient.getHotelByIdWrapped(dto.getHotelId()).getData();
                dto.setHotelName(hotel.getName());
            } catch (Exception e) {
                log.warn("Failed to fetch hotel name for hotelId: {}", dto.getHotelId());
                dto.setHotelName("Hotel #" + dto.getHotelId());
            }
        }

        return analytics;
    }

    @Override
    @Transactional(readOnly = true)
    public HotelAnalyticsDto getHotelAnalyticsById(Long hotelId) {
        log.info("Fetching analytics for hotel {}", hotelId);

        List<HotelAnalyticsDto> allAnalytics = bookingRepository.getRevenueByHotel();
        HotelAnalyticsDto hotelAnalytics = allAnalytics.stream()
                .filter(dto -> dto.getHotelId().equals(hotelId))
                .findFirst()
                .orElse(new HotelAnalyticsDto(hotelId, 0L, 0.0, 0L));

        // enrich with hotel name
        try {
            HotelDto hotel = hotelServiceClient.getHotelByIdWrapped(hotelId).getData();
            hotelAnalytics.setHotelName(hotel.getName());
        } catch (Exception e) {
            log.warn("Failed to fetch hotel name for hotelId: {}", hotelId);
            hotelAnalytics.setHotelName("Hotel #" + hotelId);
        }

        return hotelAnalytics;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RevenueByDateDto> getRevenueByDateRange(LocalDate startDate, LocalDate endDate) {
        log.info("Fetching revenue from {} to {}", startDate, endDate);
        return bookingRepository.getRevenueByDateRange(startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RevenueByDateDto> getRevenueByDateRangeForHotel(Long hotelId, LocalDate startDate, LocalDate endDate) {
        log.info("Fetching revenue for hotel {} from {} to {}", hotelId, startDate, endDate);
        return bookingRepository.getRevenueByDateRangeForHotel(hotelId, startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomTypeAnalyticsDto> getRoomTypeAnalytics() {
        log.info("Fetching room type analytics");

        // get all bookings with room details
        List<Object[]> results = bookingRepository.findAll().stream()
                .filter(b -> !b.getStatus().name().equals("CANCELLED"))
                .map(b -> {
                    try {
                        RoomDto room = hotelServiceClient.getRoomById(b.getRoomId());
                        return new Object[]{room.getRoomType(), b.getTotalAmount()};
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(obj -> obj != null)
                .collect(Collectors.toList());

        // group by room type
        Map<String, List<Float>> revenueByType = results.stream()
                .collect(Collectors.groupingBy(
                        obj -> (String) obj[0],
                        Collectors.mapping(obj -> (Float) obj[1], Collectors.toList())
                ));

        List<RoomTypeAnalyticsDto> analytics = new ArrayList<>();
        for (Map.Entry<String, List<Float>> entry : revenueByType.entrySet()) {
            String roomType = entry.getKey();
            List<Float> revenues = entry.getValue();
            Long count = (long) revenues.size();
            Double totalRevenue = revenues.stream().mapToDouble(Float::doubleValue).sum();
            Double avgPrice = totalRevenue / count;

            analytics.add(new RoomTypeAnalyticsDto(roomType, count, totalRevenue, avgPrice));
        }
        // sort by booking count descending
        analytics.sort((a, b) -> b.getBookingCount().compareTo(a.getBookingCount()));
        return analytics;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomTypeAnalyticsDto> getRoomTypeAnalyticsForHotel(Long hotelId) {
        log.info("Fetching room type analytics for hotel {}", hotelId);
        List<Object[]> results = bookingRepository.findByHotelIdOrderByCreatedAtDesc(hotelId).stream()
                .filter(b -> !b.getStatus().name().equals("CANCELLED"))
                .map(b -> {
                    try {
                        RoomDto room = hotelServiceClient.getRoomById(b.getRoomId());
                        return new Object[]{room.getRoomType(), b.getTotalAmount()};
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(obj -> obj != null)
                .collect(Collectors.toList());
        Map<String, List<Float>> revenueByType = results.stream()
                .collect(Collectors.groupingBy(
                        obj -> (String) obj[0],
                        Collectors.mapping(obj -> (Float) obj[1], Collectors.toList())
                ));

        List<RoomTypeAnalyticsDto> analytics = new ArrayList<>();
        for (Map.Entry<String, List<Float>> entry : revenueByType.entrySet()) {
            String roomType = entry.getKey();
            List<Float> revenues = entry.getValue();
            Long count = (long) revenues.size();
            Double totalRevenue = revenues.stream().mapToDouble(Float::doubleValue).sum();
            Double avgPrice = totalRevenue / count;
            analytics.add(new RoomTypeAnalyticsDto(roomType, count, totalRevenue, avgPrice));
        }
        analytics.sort((a, b) -> b.getBookingCount().compareTo(a.getBookingCount()));
        return analytics;
    }
}