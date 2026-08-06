package com.giriraj.userservice.controller;

import com.giriraj.userservice.dto.ApiResponse;
import com.giriraj.userservice.dto.UserRequestDTO;
import com.giriraj.userservice.dto.UserResponseDTO;
import com.giriraj.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;



    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponseDTO>>>
    getAllUsers() {

        List<UserResponseDTO> users =
                userService.getAllUsers();

        ApiResponse<List<UserResponseDTO>> response =
                new ApiResponse<>(
                        true,
                        "Users fetched successfully",
                        users,
                        LocalDateTime.now()
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponseDTO>>
    getUserById(
            @PathVariable Long id
    ) {

        UserResponseDTO user =
                userService.getUserById(id);

        ApiResponse<UserResponseDTO> response =
                new ApiResponse<>(
                        true,
                        "User fetched successfully",
                        user,
                        LocalDateTime.now()
                );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponseDTO>>
    updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserRequestDTO request
    ) {

        UserResponseDTO user =
                userService.updateUser(id, request);

        ApiResponse<UserResponseDTO> response =
                new ApiResponse<>(
                        true,
                        "User updated successfully",
                        user,
                        LocalDateTime.now()
                );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable Long id
    ) {

        userService.deleteUser(id);

        ApiResponse<Void> response =
                new ApiResponse<>(
                        true,
                        "User deleted successfully",
                        null,
                        LocalDateTime.now()
                );

        return ResponseEntity.ok(response);
    }
}