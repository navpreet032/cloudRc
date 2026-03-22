package com.cloudrc.server.dto;

import com.cloudrc.server.enums.BookingStatus;
import com.cloudrc.server.enums.CarStatus;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingResponse {
    private Long id;
    private BookingStatus status;
    private Integer queuePosition;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private UserSummary user;
    private CarSummary car;

    @Data
    public static class UserSummary {
        private Long id;
        private String email;
        private String role;
    }

    @Data
    public static class CarSummary {
        private Long id;
        private String name;
        private CarStatus status;
    }
}