package com.giriraj.userservice.service;

import com.giriraj.userservice.dto.UserResponseDTO;
import com.giriraj.userservice.entity.User;
import com.giriraj.userservice.exception.ResourceNotFoundException;
import com.giriraj.userservice.mapper.UserMapper;
import com.giriraj.userservice.repository.UserRepository;
import com.giriraj.userservice.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private UserResponseDTO response;

    @BeforeEach
    void setUp() {

        user = new User();
        user.setId(1L);
        user.setFirstName("Giriraj");
        user.setLastName("Singh");
        user.setPhone("9876543210");
        user.setEmail("giriraj@example.com");

        response = new UserResponseDTO();
        response.setId(1L);
        response.setFirstName("Giriraj");
        response.setLastName("Singh");
        response.setPhone("9876543210");
        response.setEmail("giriraj@example.com");
    }

    @Test
    void getUserById_whenUserExists_shouldReturnUser() {

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(userMapper.toResponseDTO(user))
                .thenReturn(response);

        UserResponseDTO result =
                userService.getUserById(1L);

        assertSame(response, result);

        verify(userRepository).findById(1L);
        verify(userMapper).toResponseDTO(user);
    }

    @Test
    void getUserById_whenUserDoesNotExist_shouldThrowNotFound() {

        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> userService.getUserById(99L)
        );

        verify(userRepository).findById(99L);

        verify(userMapper, never())
                .toResponseDTO(any(User.class));
    }

    @Test
    void deleteUser_whenUserExists_shouldDeleteUser() {

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        userService.deleteUser(1L);

        verify(userRepository).findById(1L);
        verify(userRepository).delete(user);
    }

    @Test
    void deleteUser_whenUserDoesNotExist_shouldThrowNotFound() {

        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> userService.deleteUser(99L)
        );

        verify(userRepository).findById(99L);

        verify(userRepository, never())
                .delete(any(User.class));
    }
}