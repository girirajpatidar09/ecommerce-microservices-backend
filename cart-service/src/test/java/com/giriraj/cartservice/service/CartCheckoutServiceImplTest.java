package com.giriraj.cartservice.service;

import com.giriraj.cartservice.dto.checkout.CheckoutCartDTO;
import com.giriraj.cartservice.entity.CartItem;
import com.giriraj.cartservice.exception.CartEmptyException;
import com.giriraj.cartservice.repository.CartItemRepository;
import com.giriraj.cartservice.service.impl.CartCheckoutServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartCheckoutServiceImplTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @InjectMocks
    private CartCheckoutServiceImpl checkoutService;

    @Test
    void getCheckoutCart_shouldReturnRawCartItems() {

        CartItem firstItem = createCartItem(
                1L,
                10L,
                2
        );

        CartItem secondItem = createCartItem(
                1L,
                20L,
                3
        );

        when(cartItemRepository
                .findAllByUserIdOrderByCreatedAtAsc(1L))
                .thenReturn(
                        List.of(firstItem, secondItem)
                );

        CheckoutCartDTO result =
                checkoutService.getCheckoutCart(1L);

        assertEquals(1L, result.getUserId());
        assertEquals(2, result.getItems().size());

        assertEquals(
                10L,
                result.getItems().get(0).getProductId()
        );

        assertEquals(
                2,
                result.getItems().get(0).getQuantity()
        );

        assertEquals(
                20L,
                result.getItems().get(1).getProductId()
        );

        assertEquals(
                3,
                result.getItems().get(1).getQuantity()
        );
    }

    @Test
    void getCheckoutCart_whenCartIsEmpty_shouldThrow() {

        when(cartItemRepository
                .findAllByUserIdOrderByCreatedAtAsc(1L))
                .thenReturn(List.of());

        assertThrows(
                CartEmptyException.class,
                () -> checkoutService.getCheckoutCart(1L)
        );
    }

    @Test
    void clearCheckoutCart_shouldDeleteOnlyUserCart() {

        checkoutService.clearCheckoutCart(1L);

        verify(cartItemRepository)
                .deleteAllByUserId(1L);

        verify(cartItemRepository, never())
                .deleteAllByUserId(2L);
    }

    private CartItem createCartItem(
            Long userId,
            Long productId,
            Integer quantity
    ) {

        CartItem cartItem = new CartItem();

        cartItem.setUserId(userId);
        cartItem.setProductId(productId);
        cartItem.setQuantity(quantity);

        return cartItem;
    }
}