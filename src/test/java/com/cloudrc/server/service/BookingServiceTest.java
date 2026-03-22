package com.cloudrc.server.service;

import com.cloudrc.server.enums.BookingStatus;
import com.cloudrc.server.enums.CarStatus;
import com.cloudrc.server.enums.UserRoles;
import com.cloudrc.server.exception.DuplicateResourceException;
import com.cloudrc.server.exception.ResourceNotFoundException;
import com.cloudrc.server.model.Booking;
import com.cloudrc.server.model.Car;
import com.cloudrc.server.model.User;
import com.cloudrc.server.repository.BookingRepository;
import com.cloudrc.server.repository.CarRepository;
import com.cloudrc.server.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private CarRepository carRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TaskScheduler taskScheduler;

    @InjectMocks
    private BookingService bookingService;

    private User testUser;
    private Car testCar;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@test.com");
        testUser.setRole(UserRoles.USER);

        testCar = new Car();
        testCar.setId(1L);
        testCar.setName("H12P-01");
        testCar.setStatus(CarStatus.IDLE);
    }

    // ── createBooking tests ───────────────────────────────

    @Test
    void createBooking_carIdle_returnsActiveBooking() {
        // Arrange — set up what mocks return
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(carRepository.findById(1L)).thenReturn(Optional.of(testCar));
        when(bookingRepository.findByUserAndBookingStatus(testUser, BookingStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(bookingRepository.findByUserAndBookingStatus(testUser, BookingStatus.QUEUED))
                .thenReturn(Optional.empty());
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act — call the method
        Booking result = bookingService.createBooking(1L, 1L);

        // Assert — check the result
        assertEquals(BookingStatus.ACTIVE, result.getBookingStatus());
        assertNotNull(result.getStartTime());
        assertNotNull(result.getEndTime());
        assertNull(result.getQueuePosition());
        verify(carRepository, times(1)).save(any(Car.class)); // car saved twice — status update
    }

    @Test
    void createBooking_carInUse_returnsQueuedBooking() {
        // Arrange
        testCar.setStatus(CarStatus.IN_USE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(carRepository.findById(1L)).thenReturn(Optional.of(testCar));
        when(bookingRepository.findByUserAndBookingStatus(testUser, BookingStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(bookingRepository.findByUserAndBookingStatus(testUser, BookingStatus.QUEUED))
                .thenReturn(Optional.empty());
        when(bookingRepository.countByCarAndBookingStatusAndCreatedAtBefore(
                any(), any(), any())).thenReturn(2);
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Booking result = bookingService.createBooking(1L, 1L);

        // Assert
        assertEquals(BookingStatus.QUEUED, result.getBookingStatus());
        assertEquals(3, result.getQueuePosition()); // 2 ahead + 1
        assertNull(result.getStartTime());
        assertNull(result.getEndTime());
    }

    @Test
    void createBooking_carOffline_throwsException() {
        // Arrange
        testCar.setStatus(CarStatus.OFFLINE);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(carRepository.findById(1L)).thenReturn(Optional.of(testCar));

        // Act + Assert
        assertThrows(RuntimeException.class,
                () -> bookingService.createBooking(1L, 1L));
    }

    @Test
    void createBooking_userAlreadyBooked_throwsDuplicateException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(carRepository.findById(1L)).thenReturn(Optional.of(testCar));
        when(bookingRepository.findByUserAndBookingStatus(testUser, BookingStatus.ACTIVE))
                .thenReturn(Optional.of(new Booking())); // already has active booking

        // Act + Assert
        assertThrows(DuplicateResourceException.class,
                () -> bookingService.createBooking(1L, 1L));
    }

    @Test
    void createBooking_userNotFound_throwsException() {
        // Arrange
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(ResourceNotFoundException.class,
                () -> bookingService.createBooking(99L, 1L));
    }

    // ── cancelBooking tests ───────────────────────────────

    @Test
    void cancelBooking_activeBooking_cancelsAndPromotesQueue() {
        // Arrange
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setUser(testUser);
        booking.setCar(testCar);
        booking.setBookingStatus(BookingStatus.ACTIVE);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.findByCarAndBookingStatusOrderByCreatedAtAsc(
                testCar, BookingStatus.QUEUED)).thenReturn(List.of());
        when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Act
        bookingService.cancelBooking(1L, 1L);

        // Assert
        assertEquals(BookingStatus.CANCELLED, booking.getBookingStatus());
        verify(bookingRepository).save(booking);
    }

    @Test
    void cancelBooking_wrongUser_throwsException() {
        // Arrange
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setUser(testUser); // owned by user 1
        booking.setBookingStatus(BookingStatus.ACTIVE);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        // Act + Assert — user 2 tries to cancel user 1's booking
        assertThrows(RuntimeException.class,
                () -> bookingService.cancelBooking(1L, 2L));
    }

    // ── expireBooking tests ───────────────────────────────

    @Test
    void expireBooking_promotesNextInQueue() {
        // Arrange
        Booking active = new Booking();
        active.setId(1L);
        active.setUser(testUser);
        active.setCar(testCar);
        active.setBookingStatus(BookingStatus.ACTIVE);

        User nextUser = new User();
        nextUser.setId(2L);

        Booking queued = new Booking();
        queued.setId(2L);
        queued.setUser(nextUser);
        queued.setCar(testCar);
        queued.setBookingStatus(BookingStatus.QUEUED);
        queued.setQueuePosition(1);
        queued.setCreatedAt(LocalDateTime.now());

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(active));
        when(bookingRepository.findByCarAndBookingStatusOrderByCreatedAtAsc(
                testCar, BookingStatus.QUEUED)).thenReturn(List.of(queued));
        when(bookingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Act
        bookingService.expireBooking(1L);

        // Assert
        assertEquals(BookingStatus.EXPIRED, active.getBookingStatus());
        assertEquals(BookingStatus.ACTIVE, queued.getBookingStatus());
        assertNotNull(queued.getStartTime());
        assertNotNull(queued.getEndTime());
    }
}