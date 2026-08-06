package com.giriraj.productservice.dto.stock;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockRequestDTO {

    @NotEmpty(message = "Stock items are required")
    @Valid
    private List<StockItemRequestDTO> items;
}
