package com.giriraj.productservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.validator.constraints.URL;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageDTO {

    @NotBlank(message = "Image URL is required")
    @Size(
            max = 2048,
            message = "Image URL cannot exceed 2048 characters"
    )
    @URL(message = "Image URL must be valid")
    private String imageUrl;
}