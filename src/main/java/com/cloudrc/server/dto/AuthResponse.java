package com.cloudrc.server.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuthResponse {
    private Long id;
    private String email;
    private String role;
    private LocalDateTime createdAt;
}