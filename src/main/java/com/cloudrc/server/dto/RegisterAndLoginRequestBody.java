package com.cloudrc.server.dto;

import lombok.Data;

@Data
public class RegisterAndLoginRequestBody {
    private String email;
    private String password;
}
