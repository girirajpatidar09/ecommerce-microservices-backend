package com.giriraj.userservice.service.impl;

import com.giriraj.userservice.dto.AuthResponseDTO;
import com.giriraj.userservice.dto.LoginRequestDTO;
import com.giriraj.userservice.dto.RegisterRequestDTO;
import com.giriraj.userservice.dto.UserResponseDTO;
import com.giriraj.userservice.entity.User;
import com.giriraj.userservice.enums.UserRole;
import com.giriraj.userservice.exception.EmailAlreadyExistsException;
import com.giriraj.userservice.exception.InvalidCredentialsException;
import com.giriraj.userservice.exception.PhoneAlreadyExistsException;
import com.giriraj.userservice.mapper.UserMapper;
import com.giriraj.userservice.repository.UserRepository;
import com.giriraj.userservice.service.AuthService;
import com.giriraj.userservice.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public UserResponseDTO register(
            RegisterRequestDTO request
    ) {

        String normalizedEmail =
                normalizeEmail(request.getEmail());

        if (userRepository.existsByEmailIgnoreCase(
                normalizedEmail
        )) {
            throw new EmailAlreadyExistsException(
                    "Email already exists"
            );
        }

        if (userRepository.existsByPhone(
                request.getPhone()
        )) {
            throw new PhoneAlreadyExistsException(
                    "Phone number already exists"
            );
        }

        User user = userMapper.toEntity(request);

        user.setEmail(normalizedEmail);

        user.setPasswordHash(
                passwordEncoder.encode(
                        request.getPassword()
                )
        );

        user.setRole(UserRole.CUSTOMER);

        User savedUser = userRepository.save(user);

        return userMapper.toResponseDTO(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponseDTO login(
            LoginRequestDTO request
    ) {

        String normalizedEmail =
                normalizeEmail(request.getEmail());

        User user = userRepository
                .findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() ->
                        new InvalidCredentialsException(
                                "Invalid email or password"
                        )
                );

        boolean passwordMatches =
                passwordEncoder.matches(
                        request.getPassword(),
                        user.getPasswordHash()
                );

        if (!passwordMatches) {
            throw new InvalidCredentialsException(
                    "Invalid email or password"
            );
        }

        String token = jwtService.generateToken(user);

        return new AuthResponseDTO(
                token,
                "Bearer",
                user.getId(),
                user.getEmail(),
                user.getRole()
        );
    }

    private String normalizeEmail(String email) {

        return email.trim()
                .toLowerCase(Locale.ROOT);
    }
}