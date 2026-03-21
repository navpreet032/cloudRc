package com.cloudrc.server.service;

import com.cloudrc.server.enums.BookingStatus;
import com.cloudrc.server.enums.CarStatus;
import com.cloudrc.server.exception.DuplicateResourceException;
import com.cloudrc.server.exception.ResourceNotFoundException;
import com.cloudrc.server.model.Booking;
import com.cloudrc.server.model.Car;
import com.cloudrc.server.model.User;
import com.cloudrc.server.repository.BookingRepository;
import com.cloudrc.server.repository.CarRepository;
import com.cloudrc.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;


@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private UserRepository userRepository;

    // ── Create booking ────────────────────────────────────
    public Booking createBooking(Long userId, Long carId) {

        // 1. Load user and car — throw if not found
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new ResourceNotFoundException("Car not found"));

        // 2. Check car is not OFFLINE
        if (car.getStatus() == CarStatus.OFFLINE) {
            throw new RuntimeException("Car is offline");
        }

        // 3. Check user doesn't already have active/queued booking
        boolean alreadyBooked = bookingRepository
                .findByUserAndBookingStatus(user, BookingStatus.ACTIVE)
                .isPresent()
                ||
                bookingRepository
                        .findByUserAndBookingStatus(user, BookingStatus.QUEUED)
                        .isPresent();

        if (alreadyBooked) {
            throw new DuplicateResourceException("User already has an active or queued booking");
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setCar(car);

        // 4. Car is IDLE — give immediately
        if (car.getStatus() == CarStatus.IDLE) {
            booking.setBookingStatus(BookingStatus.ACTIVE);
            booking.setStartTime(LocalDateTime.now());
            booking.setEndTime(LocalDateTime.now().plusMinutes(30));
            booking.setQueuePosition(null);

            // Update car status
            car.setStatus(CarStatus.IN_USE);
            carRepository.save(car);

        } else {
            // 5. Car is IN_USE — add to queue
            int position = bookingRepository.countByCarAndBookingStatusAndCreatedAtBefore(
                    car,
                    BookingStatus.QUEUED,
                    LocalDateTime.now()
            ) + 1; // +1 because count is people ahead, position is 1-based

            booking.setBookingStatus(BookingStatus.QUEUED);
            booking.setQueuePosition(position);
            booking.setStartTime(null);
            booking.setEndTime(null);
        }

        return bookingRepository.save(booking);
    }

    // ── Cancel booking ────────────────────────────────────
    public void cancelBooking(Long bookingId, Long userId) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        // Only the owner can cancel
        if (!booking.getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to cancel this booking");
        }

        // Can't cancel already expired/cancelled
        if (booking.getBookingStatus() == BookingStatus.EXPIRED ||
                booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new RuntimeException("Booking already ended");
        }

        boolean wasActive = booking.getBookingStatus() == BookingStatus.ACTIVE;
        booking.setBookingStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        // If active booking cancelled — free the car + promote queue
        if (wasActive) {
            Car car = booking.getCar();
            promoteNextInQueue(car);
        }
    }

    // ── Get my booking ────────────────────────────────────
    public Booking getMyBooking(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check active first, then queued
        return bookingRepository.findByUserAndBookingStatus(user, BookingStatus.ACTIVE)
                .orElseGet(() ->
                        bookingRepository.findByUserAndBookingStatus(user, BookingStatus.QUEUED)
                                .orElseThrow(() -> new ResourceNotFoundException("No active booking found"))
                );
    }

    // ── Promote next in queue (internal) ─────────────────
    public void promoteNextInQueue(Car car) {

        // Find next QUEUED booking ordered by createdAt
        List<Booking> queue = bookingRepository
                .findByCarAndBookingStatusOrderByCreatedAtAsc(car, BookingStatus.QUEUED);

        if (queue.isEmpty()) {
            // Nobody waiting — car goes idle
            car.setStatus(CarStatus.IDLE);
            carRepository.save(car);
            return;
        }

        // Promote first in queue
        Booking next = queue.get(0);
        next.setBookingStatus(BookingStatus.ACTIVE);
        next.setStartTime(LocalDateTime.now());
        next.setEndTime(LocalDateTime.now().plusMinutes(30));
        next.setQueuePosition(null);
        bookingRepository.save(next);

        // Update queue positions for everyone else
        for (int i = 1; i < queue.size(); i++) {
            queue.get(i).setQueuePosition(i);
            bookingRepository.save(queue.get(i));
        }

        // Car stays IN_USE
        car.setStatus(CarStatus.IN_USE);
        carRepository.save(car);
    }

    // ── Expire booking (called by scheduler) ─────────────
    public void expireBooking(Long bookingId) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        booking.setBookingStatus(BookingStatus.EXPIRED);
        bookingRepository.save(booking);

        promoteNextInQueue(booking.getCar());
    }
}