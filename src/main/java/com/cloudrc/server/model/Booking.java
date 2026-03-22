package com.cloudrc.server.model;

import com.cloudrc.server.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;


@Data
@Entity
@Table(name="bookings")
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id")
    private User user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="car_id")
    private Car car;
    @Enumerated(EnumType.STRING)
    private BookingStatus bookingStatus = BookingStatus.QUEUED;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer queuePosition;
    private LocalDateTime createdAt = LocalDateTime.now();

}
