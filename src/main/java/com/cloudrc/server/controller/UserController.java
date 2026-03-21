package com.cloudrc.server.controller;

import com.cloudrc.server.dto.AuthResponse;
import com.cloudrc.server.dto.RegisterAndLoginRequestBody;
import com.cloudrc.server.model.User;
import com.cloudrc.server.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class UserController {
    @Autowired
    UserService userService;

    // fullpath will be /api/auth/register
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> handleRegister(@RequestBody RegisterAndLoginRequestBody requestBody) {
        User user = userService.register(requestBody.getEmail(),requestBody.getPassword());
        AuthResponse response = new AuthResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        response.setCreatedAt(user.getCreatedAt());

        return ResponseEntity.status(201).body(response);

    }
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> handleLogin(@RequestBody RegisterAndLoginRequestBody requestBody) {
        String token= userService.login(requestBody.getEmail(),requestBody.getPassword());
        return ResponseEntity.ok(Map.of("token", token));
    }

}

