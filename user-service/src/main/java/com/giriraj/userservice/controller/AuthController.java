package com.giriraj.userservice.controller;

import com.giriraj.userservice.dto.*;
import com.giriraj.userservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponseDTO>>
    register(
            @Valid @RequestBody RegisterRequestDTO request
    ) {

        UserResponseDTO user =
                authService.register(request);

        ApiResponse<UserResponseDTO> response =
                new ApiResponse<>(
                        true,
                        "User registered successfully",
                        user,
                        LocalDateTime.now()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDTO>>
    login(
            @Valid @RequestBody LoginRequestDTO request
    ) {

        AuthResponseDTO authResponse =
                authService.login(request);

        ApiResponse<AuthResponseDTO> response =
                new ApiResponse<>(
                        true,
                        "Login successful",
                        authResponse,
                        LocalDateTime.now()
                );

        return ResponseEntity.ok(response);
    }
}