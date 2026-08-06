package com.giriraj.orderservice.dto.remote;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockItemRequestDTO {

    private Long productId;
    private Integer quantity;
}