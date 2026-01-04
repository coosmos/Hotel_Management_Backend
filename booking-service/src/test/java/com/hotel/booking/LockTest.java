package com.hotel.booking;

import com.hotel.booking.dto.request.BookingCreateRequest;
import com.hotel.booking.exception.BookingException;
import com.hotel.booking.repository.BookingRepository;
import com.hotel.booking.service.BookingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
public class LockTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    private static final Long hotel_id = 3L;
    private static final String room_type = "SUITE";
    private static final LocalDate check_in = LocalDate.now().plusDays(3);
    private static final LocalDate check_out = LocalDate.now().plusDays(4);

    @BeforeEach
    void setup() {
        bookingRepository.deleteAll();
    }

    @AfterEach
    void cleanup() {
        bookingRepository.deleteAll();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void testBooking_pessimisticLock() throws InterruptedException {
        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger failure = new AtomicInteger(0);

        BookingCreateRequest request = BookingCreateRequest.builder()
                .hotelId(hotel_id)
                .roomType(room_type)
                .checkInDate(check_in)
                .checkOutDate(check_out)
                .guestName("Test User")
                .guestEmail("test@example.com")
                .guestPhone("1234567890")
                .numberOfGuests(2)
                .build();

        System.out.println("Launching " + threads + " concurrent booking requests...\n");
        // Launch all threads
        for (int i = 0; i < threads; i++) {
            int userId = i + 1;
            executor.submit(() -> {
                try {
                    mockRequest(userId);
                    bookingService.createBooking(request);
                    success.incrementAndGet();
                    System.out.println("Thread-" + userId + " SUCCESS");

                } catch (Exception e) {
                    failure.incrementAndGet();
                    System.out.println(" Thread-" + userId + " FAILED: " + e.getMessage());

                }finally {
                    RequestContextHolder.resetRequestAttributes();
                    latch.countDown();
                }
            });
        }
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        System.out.println("Successful bookings: " + success.get());
        System.out.println("Failed bookings:     " + failure.get());
        System.out.println("Database count:      " + bookingRepository.count());
        System.out.println("All completed:       " + completed);
        assertTrue(completed, "All threads should complete within timeout");
        assertEquals(1, success.get(), "Only 1 booking should succeed");
        assertEquals(threads - 1, failure.get(), "9 bookings should fail");
        assertEquals(1, bookingRepository.count(), "Database should have exactly 1 booking");
        System.out.println(" TEST PASSED - Lock is working!");
    }

    private void mockRequest(int userId) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-User-Id", String.valueOf(userId));
        req.addHeader("X-Username", "testuser" + userId);
        req.addHeader("X-User-Email", "testuser" + userId + "@example.com");
        req.addHeader("X-User-Role", "GUEST");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
    }
}