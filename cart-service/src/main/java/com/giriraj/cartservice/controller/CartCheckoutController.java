package com.giriraj.cartservice.controller;

import com.giriraj.cartservice.dto.ApiResponse;
import com.giriraj.cartservice.dto.checkout.CheckoutCartDTO;
import com.giriraj.cartservice.service.CartCheckoutService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Validated
@RestController
@RequestMapping("/internal/cart")
@RequiredArgsConstructor
public class CartCheckoutController {

    private final CartCheckoutService cartCheckoutService;

    @GetMapping("/{userId}/items")
    public ResponseEntity<ApiResponse<CheckoutCartDTO>>
    getCheckoutCart(
            @PathVariable
            @Positive(message = "User ID must be positive")
            Long userId
    ) {

        CheckoutCartDTO cart =
                cartCheckoutService.getCheckoutCart(userId);

        ApiResponse<CheckoutCartDTO> response =
                new ApiResponse<>(
                        true,
                        "Checkout cart fetched successfully",
                        cart,
                        LocalDateTime.now()
                );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> clearCheckoutCart(
            @PathVariable
            @Positive(message = "User ID must be positive")
            Long userId
    ) {

        cartCheckoutService.clearCheckoutCart(userId);

        return ResponseEntity.noContent().build();
    }
}