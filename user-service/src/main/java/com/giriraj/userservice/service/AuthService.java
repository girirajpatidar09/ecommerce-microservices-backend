package com.giriraj.userservice.service;

import com.giriraj.userservice.dto.AuthResponseDTO;
import com.giriraj.userservice.dto.LoginRequestDTO;
import com.giriraj.userservice.dto.RegisterRequestDTO;
import com.giriraj.userservice.dto.UserResponseDTO;

public interface AuthService {

    UserResponseDTO register(RegisterRequestDTO request);

    AuthResponseDTO login(LoginRequestDTO request);
}