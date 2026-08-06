package com.giriraj.orderservice.client;

import com.giriraj.orderservice.dto.ApiResponse;
import com.giriraj.orderservice.dto.remote.CheckoutCartDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "cart-service",
        path = "/internal/cart"
)
public interface CartClient {

    @GetMapping("/{userId}/items")
    ApiResponse<CheckoutCartDTO> getCheckoutCart(
            @PathVariable("userId") Long userId
    );

    @DeleteMapping("/{userId}")
    void clearCheckoutCart(
            @PathVariable("userId") Long userId
    );
}