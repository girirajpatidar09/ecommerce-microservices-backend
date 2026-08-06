package com.giriraj.productservice.dto;

import com.giriraj.productservice.enums.Category;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequestDTO {

    @NotBlank(message = "Product name is required")
    @Size(
            max = 100,
            message = "Product name cannot exceed 100 characters"
    )
    private String name;

    @Size(
            max = 500,
            message = "Description cannot exceed 500 characters"
    )
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(
            value = "0.01",
            message = "Price must be greater than zero"
    )
    @Digits(
            integer = 8,
            fraction = 2,
            message = "Price can contain maximum 8 integer and 2 decimal digits"
    )
    private BigDecimal price;

    @NotNull(message = "Stock quantity is required")
    @PositiveOrZero(
            message = "Stock quantity cannot be negative"
    )
    private Integer stockQuantity;

    @NotNull(message = "Category is required")
    private Category category;

    @Valid
    @Size(
            max = 10,
            message = "Maximum 10 product images are allowed"
    )
    private List<ProductImageDTO> images;
}