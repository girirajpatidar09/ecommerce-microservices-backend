package com.giriraj.orderservice.dto.remote;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutCartDTO {

    private Long userId;
    private List<CheckoutCartItemDTO> items;
}