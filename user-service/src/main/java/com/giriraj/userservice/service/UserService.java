package com.giriraj.userservice.service;

import com.giriraj.userservice.dto.UserRequestDTO;
import com.giriraj.userservice.dto.UserResponseDTO;

import java.util.List;

public interface UserService {

    List<UserResponseDTO> getAllUsers();

    UserResponseDTO getUserById(Long id);

    UserResponseDTO updateUser(
            Long id,
            UserRequestDTO request
    );

    void deleteUser(Long id);
}