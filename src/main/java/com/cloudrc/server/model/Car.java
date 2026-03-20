package com.cloudrc.server.model;

import com.cloudrc.server.enums.CarStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;


@Entity
@Data
@Table(name = "cars")
public class Car {
    @Id
    private Long id;
    private String name;
    private CarStatus status=CarStatus.OFFLINE;
    private LocalDateTime lastSeen;
}
