package com.giriraj.cartservice.service.impl;

import com.giriraj.cartservice.dto.checkout.CheckoutCartDTO;
import com.giriraj.cartservice.dto.checkout.CheckoutCartItemDTO;
import com.giriraj.cartservice.entity.CartItem;
import com.giriraj.cartservice.exception.CartEmptyException;
import com.giriraj.cartservice.repository.CartItemRepository;
import com.giriraj.cartservice.service.CartCheckoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartCheckoutServiceImpl
        implements CartCheckoutService {

    private final CartItemRepository cartItemRepository;

    @Override
    @Transactional(readOnly = true)
    public CheckoutCartDTO getCheckoutCart(Long userId) {

        List<CartItem> cartItems =
                cartItemRepository
                        .findAllByUserIdOrderByCreatedAtAsc(
                                userId
                        );

        if (cartItems.isEmpty()) {
            throw new CartEmptyException(
                    "Cart is empty. Add items before checkout"
            );
        }

        List<CheckoutCartItemDTO> items =
                cartItems.stream()
                        .map(cartItem ->
                                new CheckoutCartItemDTO(
                                        cartItem.getProductId(),
                                        cartItem.getQuantity()
                                )
                        )
                        .toList();

        return new CheckoutCartDTO(
                userId,
                items
        );
    }

    @Override
    @Transactional
    public void clearCheckoutCart(Long userId) {

        cartItemRepository.deleteAllByUserId(userId);
    }
}