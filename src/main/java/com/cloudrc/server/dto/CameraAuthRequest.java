package com.cloudrc.server.dto;

import lombok.Data;

@Data
public class CameraAuthRequest {
    private String carId;
    private String adminToken;
}
