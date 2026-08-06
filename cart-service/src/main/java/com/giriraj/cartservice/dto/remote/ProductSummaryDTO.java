package com.giriraj.cartservice.dto.remote;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductSummaryDTO {

    private Long id;
    private String name;
    private BigDecimal price;
    private Integer stockQuantity;
    private Boolean active;
    private List<ProductImageSummaryDTO> images;
}