package com.giriraj.userservice.service;

import com.giriraj.userservice.dto.AddressDTO;
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
import com.giriraj.userservice.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequestDTO registerRequest;
    private User user;
    private UserResponseDTO userResponse;

    @BeforeEach
    void setUp() {

        AddressDTO address = new AddressDTO(
                "Main Road",
                "Jaipur",
                "Rajasthan",
                "India",
                "302001"
        );

        registerRequest = new RegisterRequestDTO(
                "Giriraj",
                "Singh",
                "9876543210",
                " GIRIRAJ@EXAMPLE.COM ",
                "Password@123",
                address
        );

        user = new User();
        user.setId(1L);
        user.setFirstName("Giriraj");
        user.setLastName("Singh");
        user.setPhone("9876543210");
        user.setEmail("giriraj@example.com");

        userResponse = new UserResponseDTO();
        userResponse.setId(1L);
        userResponse.setEmail(
                "giriraj@example.com"
        );
    }

    @Test
    void register_shouldHashPasswordAndSaveCustomer() {

        when(userRepository.existsByEmailIgnoreCase(
                "giriraj@example.com"
        )).thenReturn(false);

        when(userRepository.existsByPhone(
                "9876543210"
        )).thenReturn(false);

        when(userMapper.toEntity(registerRequest))
                .thenReturn(user);

        when(passwordEncoder.encode(
                "Password@123"
        )).thenReturn("hashed-password");

        when(userRepository.save(user))
                .thenReturn(user);

        when(userMapper.toResponseDTO(user))
                .thenReturn(userResponse);

        UserResponseDTO result =
                authService.register(
                        registerRequest
                );

        assertSame(userResponse, result);

        assertEquals(
                "giriraj@example.com",
                user.getEmail()
        );

        assertEquals(
                "hashed-password",
                user.getPasswordHash()
        );

        assertEquals(
                UserRole.CUSTOMER,
                user.getRole()
        );

        verify(passwordEncoder).encode(
                "Password@123"
        );

        verify(userRepository).save(user);
    }

    @Test
    void register_whenEmailExists_shouldThrow() {

        when(userRepository.existsByEmailIgnoreCase(
                "giriraj@example.com"
        )).thenReturn(true);

        assertThrows(
                EmailAlreadyExistsException.class,
                () -> authService.register(
                        registerRequest
                )
        );

        verify(userRepository, never())
                .save(any(User.class));

        verifyNoInteractions(
                userMapper,
                passwordEncoder,
                jwtService
        );
    }

    @Test
    void register_whenPhoneExists_shouldThrow() {

        when(userRepository.existsByEmailIgnoreCase(
                "giriraj@example.com"
        )).thenReturn(false);

        when(userRepository.existsByPhone(
                "9876543210"
        )).thenReturn(true);

        assertThrows(
                PhoneAlreadyExistsException.class,
                () -> authService.register(
                        registerRequest
                )
        );

        verify(userRepository, never())
                .save(any(User.class));

        verifyNoInteractions(
                userMapper,
                passwordEncoder,
                jwtService
        );
    }

    @Test
    void login_withCorrectPassword_shouldReturnToken() {

        LoginRequestDTO loginRequest =
                new LoginRequestDTO(
                        " GIRIRAJ@EXAMPLE.COM ",
                        "Password@123"
                );

        user.setPasswordHash(
                "hashed-password"
        );

        user.setRole(UserRole.CUSTOMER);

        when(userRepository.findByEmailIgnoreCase(
                "giriraj@example.com"
        )).thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "Password@123",
                "hashed-password"
        )).thenReturn(true);

        when(jwtService.generateToken(user))
                .thenReturn("jwt-token");

        AuthResponseDTO response =
                authService.login(loginRequest);

        assertEquals(
                "jwt-token",
                response.getToken()
        );

        assertEquals(
                "Bearer",
                response.getTokenType()
        );

        assertEquals(
                1L,
                response.getUserId()
        );

        assertEquals(
                UserRole.CUSTOMER,
                response.getRole()
        );

        verify(jwtService).generateToken(user);
    }

    @Test
    void login_whenEmailDoesNotExist_shouldThrow() {

        LoginRequestDTO loginRequest =
                new LoginRequestDTO(
                        "unknown@example.com",
                        "Password@123"
                );

        when(userRepository.findByEmailIgnoreCase(
                "unknown@example.com"
        )).thenReturn(Optional.empty());

        InvalidCredentialsException exception =
                assertThrows(
                        InvalidCredentialsException.class,
                        () -> authService.login(
                                loginRequest
                        )
                );

        assertEquals(
                "Invalid email or password",
                exception.getMessage()
        );

        verifyNoInteractions(
                passwordEncoder,
                jwtService
        );
    }

    @Test
    void login_withWrongPassword_shouldThrow() {

        LoginRequestDTO loginRequest =
                new LoginRequestDTO(
                        "giriraj@example.com",
                        "WrongPassword"
                );

        user.setPasswordHash(
                "hashed-password"
        );

        when(userRepository.findByEmailIgnoreCase(
                "giriraj@example.com"
        )).thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "WrongPassword",
                "hashed-password"
        )).thenReturn(false);

        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(
                        loginRequest
                )
        );

        verifyNoInteractions(jwtService);
    }
}