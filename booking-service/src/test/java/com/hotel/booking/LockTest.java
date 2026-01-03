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
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
public class LockTest {
    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    private static final Long hotel_id=3L;
    private static final Long room_id=2L;
    private static final LocalDate check_in= LocalDate.now().plusDays(3);
    private static final LocalDate check_out= LocalDate.now().plusDays(4);

    @BeforeEach
    void setup(){
        bookingRepository.deleteAll();
    }
    @AfterEach
    void cleanup(){
        bookingRepository.deleteAll();
        RequestContextHolder.resetRequestAttributes();  //to access http request user id and and role
    }
    @Test
    void testBooking_pessimisticLock()throws InterruptedException{
        int threads=10;
        ExecutorService executor= Executors.newFixedThreadPool(threads);
        CountDownLatch latch=new CountDownLatch(threads);// wait for all threads to finisj
        AtomicInteger success=new AtomicInteger();
        AtomicInteger failure =new AtomicInteger();
        BookingCreateRequest request=BookingCreateRequest.builder()
                .hotelId(hotel_id)
                .roomId(room_id)
                .checkInDate(check_in)
                .checkOutDate(check_out)
                .guestName("Test User")
                .guestEmail("test@example.com")
                .guestPhone("1234567890")
                .numberOfGuests(2)
                .build();
    for(int i=0; i<threads; i++){
        int userId=i+1;
        executor.submit(()->{
            try{
                mockRequest(userId);

            }catch (BookingException e){
                failure.incrementAndGet();
            }finally{
                RequestContextHolder.resetRequestAttributes();
                latch.countDown();
            }
        });
    }
    latch.await();
    executor.shutdown();
    assertEquals(1, success.get());
    assertEquals(threads-1,failure.get());
    assertEquals(1, bookingRepository.count());
    }

    private void mockRequest(int userId){
        MockHttpServletRequest req= new MockHttpServletRequest();
        req.addHeader("X-User-Id",String.valueOf(userId));
        req.addHeader("X-User-Role","GUEST");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
    }
}
