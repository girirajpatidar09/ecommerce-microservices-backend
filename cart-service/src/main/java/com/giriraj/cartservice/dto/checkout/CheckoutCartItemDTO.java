package com.giriraj.cartservice.dto.checkout;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutCartItemDTO {

    private Long productId;
    private Integer quantity;
}