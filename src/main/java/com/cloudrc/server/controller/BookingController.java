package com.cloudrc.server.controller;

import com.cloudrc.server.dto.BookingResponse;
import com.cloudrc.server.dto.CreateBookingPyload;
import com.cloudrc.server.model.Booking;
import com.cloudrc.server.model.User;
import com.cloudrc.server.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    @Autowired
    BookingService bookingService;

    // act as "api/bookings/"
    @PostMapping
    public ResponseEntity<Booking> createBooking(Authentication authentication, @RequestBody CreateBookingPyload pyload) {
        User user = (User) authentication.getPrincipal();
        Long carId = pyload.getCarId();
        Long userId = user.getId();
        System.out.println("carId: " + carId);
        System.out.println("userId: " + userId);
        return ResponseEntity.status(201).body(bookingService.createBooking(userId, carId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBooking(Authentication authentication, @PathVariable Long id) {
        User user = (User) authentication.getPrincipal();
        Long userId = user.getId();
        bookingService.cancelBooking( id,userId);
        return ResponseEntity.status(204).build();
    }
    @GetMapping("/my")
    public ResponseEntity<BookingResponse>  getMyBooking(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        Long userId = user.getId();
        Booking booking = bookingService.getMyBooking(userId);
        BookingResponse response = new BookingResponse();
        response.setId(booking.getId());
        response.setStatus(booking.getBookingStatus());
        response.setQueuePosition(booking.getQueuePosition());
        response.setStartTime(booking.getStartTime());
        response.setEndTime(booking.getEndTime());

        // map user
        BookingResponse.UserSummary userSummary = new BookingResponse.UserSummary();
        userSummary.setId(user.getId());
        userSummary.setEmail(user.getEmail());
        userSummary.setRole(user.getRole().name());
        response.setUser(userSummary);

        // map car
        BookingResponse.CarSummary carSummary = new BookingResponse.CarSummary();
        carSummary.setId(booking.getCar().getId());
        carSummary.setName(booking.getCar().getName());
        carSummary.setStatus(booking.getCar().getStatus());
        response.setCar(carSummary);

        return ResponseEntity.ok(response);

    }
}
