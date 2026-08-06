package com.giriraj.productservice.dto.stock;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockReservationResponseDTO {

    private List<StockReservationItemDTO> items;
    private BigDecimal totalAmount;
}