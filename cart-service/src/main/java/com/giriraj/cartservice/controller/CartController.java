package com.giriraj.cartservice.controller;

import com.giriraj.cartservice.dto.ApiResponse;
import com.giriraj.cartservice.dto.CartItemRequestDTO;
import com.giriraj.cartservice.dto.CartItemResponseDTO;
import com.giriraj.cartservice.service.CartService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController
{


    private final CartService cartService;

    @PostMapping
    public ResponseEntity<ApiResponse<CartItemResponseDTO>>
    addToCart(
            @RequestHeader("X-User-ID")
            @Positive(message = "User ID must be positive")
            Long userId,

            @Valid @RequestBody
            CartItemRequestDTO request
    ) {

        CartItemResponseDTO cartItem =
                cartService.addToCart(userId, request);

        ApiResponse<CartItemResponseDTO> response =
                new ApiResponse<>(
                        true,
                        "Product added to cart successfully",
                        cartItem,
                        LocalDateTime.now()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CartItemResponseDTO>>>
    getCart(
            @RequestHeader("X-User-ID")
            @Positive(message = "User ID must be positive")
            Long userId
    ) {

        List<CartItemResponseDTO> cartItems =
                cartService.getCart(userId);

        ApiResponse<List<CartItemResponseDTO>> response =
                new ApiResponse<>(
                        true,
                        "Cart fetched successfully",
                        cartItems,
                        LocalDateTime.now()
                );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<Void> removeFromCart(
            @RequestHeader("X-User-ID")
            @Positive(message = "User ID must be positive")
            Long userId,

            @PathVariable
            @Positive(message = "Product ID must be positive")
            Long productId
    ) {

        cartService.removeFromCart(userId, productId);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(
            @RequestHeader("X-User-ID")
            @Positive(message = "User ID must be positive")
            Long userId
    ) {

        cartService.clearCart(userId);

        return ResponseEntity.noContent().build();
    }
}