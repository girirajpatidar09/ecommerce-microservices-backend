package com.giriraj.cartservice.client;

import com.giriraj.cartservice.dto.ApiResponse;
import com.giriraj.cartservice.dto.remote.ProductSummaryDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "product-service",
        path = "/api/products"
)
public interface ProductClient {

    @GetMapping("/{id}")
    ApiResponse<ProductSummaryDTO> getProductById(
            @PathVariable("id") Long id
    );
}