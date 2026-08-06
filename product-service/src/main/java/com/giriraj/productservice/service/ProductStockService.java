package com.giriraj.productservice.service;

import com.giriraj.productservice.dto.stock.StockRequestDTO;
import com.giriraj.productservice.dto.stock.StockReservationResponseDTO;

public interface ProductStockService {

    StockReservationResponseDTO reserveStock(
            StockRequestDTO request
    );

    void restoreStock(StockRequestDTO request);
}