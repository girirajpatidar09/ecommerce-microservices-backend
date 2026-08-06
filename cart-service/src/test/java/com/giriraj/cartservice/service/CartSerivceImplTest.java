package com.giriraj.cartservice.service;

import com.giriraj.cartservice.client.ProductClient;
import com.giriraj.cartservice.client.UserClient;
import com.giriraj.cartservice.dto.ApiResponse;
import com.giriraj.cartservice.dto.CartItemRequestDTO;
import com.giriraj.cartservice.dto.CartItemResponseDTO;
import com.giriraj.cartservice.dto.remote.ProductImageSummaryDTO;
import com.giriraj.cartservice.dto.remote.ProductSummaryDTO;
import com.giriraj.cartservice.dto.remote.UserSummaryDTO;
import com.giriraj.cartservice.entity.CartItem;
import com.giriraj.cartservice.exception.InsufficientStockException;
import com.giriraj.cartservice.repository.CartItemRepository;
import com.giriraj.cartservice.service.impl.CartServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private UserClient userClient;

    @Mock
    private ProductClient productClient;

    @InjectMocks
    private CartServiceImpl cartService;

    private ProductSummaryDTO product;

    @BeforeEach
    void setUp() {

        product = new ProductSummaryDTO(
                10L,
                "Wireless Headphones",
                new BigDecimal("100.00"),
                5,
                true,
                List.of(
                        new ProductImageSummaryDTO(
                                "https://example.com/product.jpg"
                        )
                )
        );
    }

    @Test
    void addToCart_whenNewItem_shouldSaveAndCalculateTotal() {

        stubValidUser(1L);
        stubProduct(product);

        CartItemRequestDTO request =
                new CartItemRequestDTO(10L, 2);

        when(cartItemRepository
                .findByUserIdAndProductId(1L, 10L))
                .thenReturn(Optional.empty());

        when(cartItemRepository.save(any(CartItem.class)))
                .thenAnswer(invocation -> {

                    CartItem cartItem =
                            invocation.getArgument(0);

                    cartItem.setId(100L);

                    return cartItem;
                });

        CartItemResponseDTO result =
                cartService.addToCart(1L, request);

        assertEquals(100L, result.getId());
        assertEquals(10L, result.getProductId());
        assertEquals(2, result.getQuantity());
        assertEquals(
                "Wireless Headphones",
                result.getProductName()
        );
        assertEquals(
                "https://example.com/product.jpg",
                result.getImageUrl()
        );

        assertEquals(
                0,
                new BigDecimal("200.00")
                        .compareTo(result.getTotalPrice())
        );

        verify(cartItemRepository)
                .save(any(CartItem.class));
    }

    @Test
    void addToCart_whenItemExists_shouldUseCombinedQuantity() {

        stubValidUser(1L);
        stubProduct(product);

        CartItem existingItem = createCartItem(
                1L,
                10L,
                2
        );

        when(cartItemRepository
                .findByUserIdAndProductId(1L, 10L))
                .thenReturn(Optional.of(existingItem));

        when(cartItemRepository.save(existingItem))
                .thenReturn(existingItem);

        CartItemResponseDTO result =
                cartService.addToCart(
                        1L,
                        new CartItemRequestDTO(10L, 3)
                );

        assertEquals(5, existingItem.getQuantity());
        assertEquals(5, result.getQuantity());

        assertEquals(
                0,
                new BigDecimal("500.00")
                        .compareTo(result.getTotalPrice())
        );
    }

    @Test
    void addToCart_whenCombinedQuantityExceedsStock_shouldThrow() {

        stubValidUser(1L);
        stubProduct(product);

        CartItem existingItem = createCartItem(
                1L,
                10L,
                3
        );

        when(cartItemRepository
                .findByUserIdAndProductId(1L, 10L))
                .thenReturn(Optional.of(existingItem));

        assertThrows(
                InsufficientStockException.class,
                () -> cartService.addToCart(
                        1L,
                        new CartItemRequestDTO(10L, 3)
                )
        );

        verify(cartItemRepository, never())
                .save(any());
    }

    @Test
    void getCart_shouldEnrichItemFromProductService() {

        stubValidUser(1L);
        stubProduct(product);

        CartItem cartItem = createCartItem(
                1L,
                10L,
                2
        );

        cartItem.setId(100L);

        when(cartItemRepository
                .findAllByUserIdOrderByCreatedAtAsc(1L))
                .thenReturn(List.of(cartItem));

        List<CartItemResponseDTO> result =
                cartService.getCart(1L);

        assertEquals(1, result.size());
        assertEquals(
                "Wireless Headphones",
                result.get(0).getProductName()
        );

        assertEquals(
                0,
                new BigDecimal("200.00")
                        .compareTo(
                                result.get(0).getTotalPrice()
                        )
        );
    }

    @Test
    void removeFromCart_shouldNotCallProductService() {

        stubValidUser(1L);

        CartItem cartItem = createCartItem(
                1L,
                10L,
                2
        );

        when(cartItemRepository
                .findByUserIdAndProductId(1L, 10L))
                .thenReturn(Optional.of(cartItem));

        cartService.removeFromCart(1L, 10L);

        verify(cartItemRepository).delete(cartItem);

        /*
         * An inactive Product must still be removable.
         */
        verifyNoInteractions(productClient);
    }

    @Test
    void clearCart_shouldDeleteOnlyCurrentUserItems() {

        stubValidUser(1L);

        cartService.clearCart(1L);

        verify(cartItemRepository)
                .deleteAllByUserId(1L);
    }

    private void stubValidUser(Long userId) {

        ApiResponse<UserSummaryDTO> response =
                new ApiResponse<>(
                        true,
                        "User fetched successfully",
                        new UserSummaryDTO(userId),
                        LocalDateTime.now()
                );

        when(userClient.getUserById(userId))
                .thenReturn(response);
    }

    private void stubProduct(
            ProductSummaryDTO productSummary
    ) {

        ApiResponse<ProductSummaryDTO> response =
                new ApiResponse<>(
                        true,
                        "Product fetched successfully",
                        productSummary,
                        LocalDateTime.now()
                );

        when(productClient.getProductById(
                productSummary.getId()
        )).thenReturn(response);
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