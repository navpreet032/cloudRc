package com.cloudrc.server.model;

import com.cloudrc.server.enums.CarStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;


@Entity
@Data
@Table(name = "cars")
public class Car {
    @Id
    private Long id;
    private String name;
    @Enumerated(EnumType.STRING)// without this jpa will store it as int ( IDLE : 1)
    private CarStatus status=CarStatus.OFFLINE;
    private LocalDateTime lastSeen;
}
