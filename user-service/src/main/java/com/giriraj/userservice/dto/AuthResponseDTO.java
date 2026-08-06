package com.giriraj.userservice.dto;

import com.giriraj.userservice.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {

    private String token;

    private String tokenType;

    private Long userId;

    private String email;

    private UserRole role;
}