package com.giriraj.cartservice.service.impl;

import com.giriraj.cartservice.client.ProductClient;
import com.giriraj.cartservice.client.UserClient;
import com.giriraj.cartservice.dto.ApiResponse;
import com.giriraj.cartservice.dto.CartItemRequestDTO;
import com.giriraj.cartservice.dto.CartItemResponseDTO;
import com.giriraj.cartservice.dto.remote.ProductImageSummaryDTO;
import com.giriraj.cartservice.dto.remote.ProductSummaryDTO;
import com.giriraj.cartservice.dto.remote.UserSummaryDTO;
import com.giriraj.cartservice.entity.CartItem;
import com.giriraj.cartservice.exception.ExternalServiceException;
import com.giriraj.cartservice.exception.InsufficientStockException;
import com.giriraj.cartservice.exception.ResourceNotFoundException;
import com.giriraj.cartservice.repository.CartItemRepository;
import com.giriraj.cartservice.service.CartService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CartServiceImpl implements CartService {

    private final CartItemRepository cartItemRepository;
    private final UserClient userClient;
    private final ProductClient productClient;

    @Override
    public CartItemResponseDTO addToCart(
            Long userId,
            CartItemRequestDTO request
    ) {

        validateUser(userId);

        ProductSummaryDTO product =
                getAvailableProduct(request.getProductId());

        CartItem cartItem = cartItemRepository
                .findByUserIdAndProductId(
                        userId,
                        request.getProductId()
                )
                .orElseGet(() ->
                        createCartItem(
                                userId,
                                request.getProductId()
                        )
                );

        int combinedQuantity =
                cartItem.getQuantity()
                        + request.getQuantity();

        if (combinedQuantity
                > product.getStockQuantity()) {

            throw new InsufficientStockException(
                    "Only "
                            + product.getStockQuantity()
                            + " units are available for product: "
                            + product.getName()
            );
        }

        cartItem.setQuantity(combinedQuantity);

        CartItem savedItem =
                cartItemRepository.save(cartItem);

        return createResponse(savedItem, product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CartItemResponseDTO> getCart(Long userId) {

        validateUser(userId);

        List<CartItem> cartItems =
                cartItemRepository
                        .findAllByUserIdOrderByCreatedAtAsc(
                                userId
                        );

        return cartItems.stream()
                .map(cartItem -> {

                    ProductSummaryDTO product =
                            getAvailableProduct(
                                    cartItem.getProductId()
                            );

                    return createResponse(
                            cartItem,
                            product
                    );
                })
                .toList();
    }

    @Override
    public void removeFromCart(
            Long userId,
            Long productId
    ) {

        validateUser(userId);

        CartItem cartItem = cartItemRepository
                .findByUserIdAndProductId(
                        userId,
                        productId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product is not present in the cart"
                        )
                );

        /*
         * Product Service validation is intentionally not done.
         * An unavailable Product must still be removable.
         */
        cartItemRepository.delete(cartItem);
    }

    @Override
    public void clearCart(Long userId) {

        validateUser(userId);
        cartItemRepository.deleteAllByUserId(userId);
    }

    private CartItem createCartItem(
            Long userId,
            Long productId
    ) {

        CartItem cartItem = new CartItem();
        cartItem.setUserId(userId);
        cartItem.setProductId(productId);
        cartItem.setQuantity(0);

        return cartItem;
    }

    private UserSummaryDTO validateUser(Long userId) {

        try {

            ApiResponse<UserSummaryDTO> response =
                    userClient.getUserById(userId);

            if (response == null || !response.isSuccess() || response.getData() == null)
            {

                throw new ExternalServiceException(
                        "Invalid response received from User Service"
                );
            }

            return response.getData();

        } catch (FeignException.NotFound exception) {

            throw new ResourceNotFoundException(
                    "User not found with id: " + userId
            );

        } catch (FeignException exception) {

            throw new ExternalServiceException(
                    "User Service is currently unavailable"
            );
        }
    }

    private ProductSummaryDTO getAvailableProduct(
            Long productId
    ) {

        try {

            ApiResponse<ProductSummaryDTO> response =
                    productClient.getProductById(productId);

            if (response == null
                    || !response.isSuccess()
                    || response.getData() == null) {

                throw new ExternalServiceException(
                        "Invalid response received from Product Service"
                );
            }

            ProductSummaryDTO product = response.getData();

            if (!Boolean.TRUE.equals(product.getActive())) {

                throw new ResourceNotFoundException(
                        "Product is currently unavailable"
                );
            }

            if (product.getPrice() == null
                    || product.getStockQuantity() == null) {

                throw new ExternalServiceException(
                        "Incomplete Product information received"
                );
            }

            return product;

        } catch (FeignException.NotFound exception) {

            throw new ResourceNotFoundException(
                    "Product not found with id: " + productId
            );

        } catch (FeignException exception) {

            throw new ExternalServiceException(
                    "Product Service is currently unavailable"
            );
        }
    }

    private CartItemResponseDTO createResponse(
            CartItem cartItem,
            ProductSummaryDTO product
    ) {

        BigDecimal totalPrice = product.getPrice()
                .multiply(
                        BigDecimal.valueOf(
                                cartItem.getQuantity()
                        )
                );

        CartItemResponseDTO response =
                new CartItemResponseDTO();

        response.setId(cartItem.getId());
        response.setProductId(cartItem.getProductId());
        response.setProductName(product.getName());
        response.setImageUrl(getFirstImageUrl(product));
        response.setQuantity(cartItem.getQuantity());
        response.setUnitPrice(product.getPrice());
        response.setTotalPrice(totalPrice);

        return response;
    }

    private String getFirstImageUrl(
            ProductSummaryDTO product
    ) {

        List<ProductImageSummaryDTO> images =
                product.getImages();

        if (images == null || images.isEmpty()) {
            return null;
        }

        return images.get(0).getImageUrl();
    }
}