package com.giriraj.userservice.service.impl;

import com.giriraj.userservice.dto.AddressDTO;
import com.giriraj.userservice.dto.UserRequestDTO;
import com.giriraj.userservice.dto.UserResponseDTO;
import com.giriraj.userservice.entity.Address;
import com.giriraj.userservice.entity.User;
import com.giriraj.userservice.exception.EmailAlreadyExistsException;
import com.giriraj.userservice.exception.PhoneAlreadyExistsException;
import com.giriraj.userservice.exception.ResourceNotFoundException;
import com.giriraj.userservice.mapper.UserMapper;
import com.giriraj.userservice.repository.UserRepository;
import com.giriraj.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;



    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDTO> getAllUsers() {

        return userRepository.findAll()
                .stream()
                .map(userMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(Long id) {

        User user = findUserById(id);

        return userMapper.toResponseDTO(user);
    }

    @Override
    public UserResponseDTO updateUser(
            Long id,
            UserRequestDTO request
    ) {

        User user = findUserById(id);

        String normalizedEmail =
                normalizeEmail(request.getEmail());

        if (!user.getEmail().equalsIgnoreCase(normalizedEmail)
                && userRepository.existsByEmailIgnoreCase(
                normalizedEmail
        )) {

            throw new EmailAlreadyExistsException(
                    "Email already exists"
            );
        }

        if (!user.getPhone().equals(request.getPhone())
                && userRepository.existsByPhone(
                request.getPhone()
        )) {

            throw new PhoneAlreadyExistsException(
                    "Phone number already exists"
            );
        }

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setEmail(normalizedEmail);

        updateAddress(
                user.getAddress(),
                request.getAddress()
        );

        User updatedUser = userRepository.save(user);

        return userMapper.toResponseDTO(updatedUser);
    }

    @Override
    public void deleteUser(Long id) {

        User user = findUserById(id);
        userRepository.delete(user);
    }

    private User findUserById(Long id) {

        return userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with id: " + id
                        )
                );
    }

    private void updateAddress(
            Address address,
            AddressDTO addressDTO
    ) {

        address.setStreet(addressDTO.getStreet());
        address.setCity(addressDTO.getCity());
        address.setState(addressDTO.getState());
        address.setCountry(addressDTO.getCountry());
        address.setZipCode(addressDTO.getZipCode());
    }

    private String normalizeEmail(String email) {

        return email.trim()
                .toLowerCase(Locale.ROOT);
    }
}