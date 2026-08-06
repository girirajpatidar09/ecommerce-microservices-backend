package com.giriraj.cartservice.service;

import com.giriraj.cartservice.dto.CartItemRequestDTO;
import com.giriraj.cartservice.dto.CartItemResponseDTO;

import java.util.List;

public interface CartService {

    CartItemResponseDTO addToCart(
            Long userId,
            CartItemRequestDTO request
    );

    List<CartItemResponseDTO> getCart(Long userId);

    void removeFromCart(
            Long userId,
            Long productId
    );

    void clearCart(Long userId);
}