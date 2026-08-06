package com.giriraj.productservice.dto;

import com.giriraj.productservice.enums.Category;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDTO {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;
    private Category category;
    private Boolean active;
    private List<ProductImageDTO> images;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}