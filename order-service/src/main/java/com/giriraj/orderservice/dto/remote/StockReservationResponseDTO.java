package com.giriraj.orderservice.dto.remote;

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
