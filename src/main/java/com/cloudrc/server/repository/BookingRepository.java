package com.cloudrc.server.repository;

import com.cloudrc.server.enums.BookingStatus;
import com.cloudrc.server.model.Booking;
import com.cloudrc.server.model.Car;
import com.cloudrc.server.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    public Optional<Booking> findByUser(User user);
    public Optional<Booking> findByCar(Car car);
    public Optional<Booking> findByCarAndBookingStatus(Car car, BookingStatus bookingStatus);
    public List<Booking> findByBookingStatusOrderByCreatedAtAsc(BookingStatus bookingStatus);
    public Optional<Booking> findByUserAndBookingStatus(User user, BookingStatus bookingStatus);
    public int countByCarAndBookingStatusAndCreatedAtBefore(Car car, BookingStatus bookingStatus, LocalDateTime createdAt);

}
