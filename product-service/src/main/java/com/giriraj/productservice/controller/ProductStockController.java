package com.giriraj.productservice.controller;

import com.giriraj.productservice.dto.ApiResponse;
import com.giriraj.productservice.dto.stock.StockRequestDTO;
import com.giriraj.productservice.dto.stock.StockReservationResponseDTO;
import com.giriraj.productservice.service.ProductStockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/internal/products/stock")
@RequiredArgsConstructor
public class ProductStockController {

    private final ProductStockService productStockService;

    @PostMapping("/reserve")
    public ResponseEntity<
            ApiResponse<StockReservationResponseDTO>>
    reserveStock(
            @Valid @RequestBody StockRequestDTO request
    ) {

        StockReservationResponseDTO reservation =
                productStockService.reserveStock(request);

        ApiResponse<StockReservationResponseDTO> response =
                new ApiResponse<>(
                        true,
                        "Stock reserved successfully",
                        reservation,
                        LocalDateTime.now()
                );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/restore")
    public ResponseEntity<Void> restoreStock(
            @Valid @RequestBody StockRequestDTO request
    ) {

        productStockService.restoreStock(request);

        return ResponseEntity.noContent().build();
    }
}