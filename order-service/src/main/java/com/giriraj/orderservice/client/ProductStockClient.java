package com.giriraj.orderservice.client;

import com.giriraj.orderservice.dto.ApiResponse;
import com.giriraj.orderservice.dto.remote.StockRequestDTO;
import com.giriraj.orderservice.dto.remote.StockReservationResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "product-service",
        path = "/internal/products/stock"
)
public interface ProductStockClient {

    @PostMapping("/reserve")
    ApiResponse<StockReservationResponseDTO> reserveStock(
            @RequestBody StockRequestDTO request
    );

    @PostMapping("/restore")
    void restoreStock(
            @RequestBody StockRequestDTO request
    );
}