package com.giriraj.orderservice.dto.remote;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryDTO {

    private Long id;

    private String email;

    public UserSummaryDTO(Long id) {
        this.id = id;
    }
}
