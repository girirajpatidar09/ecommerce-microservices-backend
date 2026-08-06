package com.giriraj.orderservice.service.impl;

import com.giriraj.orderservice.client.CartClient;
import com.giriraj.orderservice.client.PaymentClient;
import com.giriraj.orderservice.client.ProductStockClient;
import com.giriraj.orderservice.client.UserClient;
import com.giriraj.orderservice.dto.ApiResponse;
import com.giriraj.orderservice.dto.CheckoutResponseDTO;
import com.giriraj.orderservice.dto.OrderResponseDTO;
import com.giriraj.orderservice.dto.remote.CheckoutCartDTO;
import com.giriraj.orderservice.dto.remote.PaymentRequestDTO;
import com.giriraj.orderservice.dto.remote.PaymentResponseDTO;
import com.giriraj.orderservice.dto.remote.StockItemRequestDTO;
import com.giriraj.orderservice.dto.remote.StockRequestDTO;
import com.giriraj.orderservice.dto.remote.StockReservationResponseDTO;
import com.giriraj.orderservice.dto.remote.UserSummaryDTO;
import com.giriraj.orderservice.exception.CartEmptyException;
import com.giriraj.orderservice.exception.CompensationFailedException;
import com.giriraj.orderservice.exception.ExternalServiceException;
import com.giriraj.orderservice.exception.InsufficientStockException;
import com.giriraj.orderservice.exception.ResourceNotFoundException;
import com.giriraj.orderservice.service.OrderCheckoutService;
import com.giriraj.orderservice.service.OrderPersistenceService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderCheckoutServiceImpl
        implements OrderCheckoutService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    OrderCheckoutServiceImpl.class
            );

    private final UserClient userClient;
    private final CartClient cartClient;
    private final ProductStockClient productStockClient;
    private final PaymentClient paymentClient;
    private final OrderPersistenceService persistenceService;

    @Override
    public CheckoutResponseDTO placeOrder(Long userId) {

        validateUser(userId);

        CheckoutCartDTO cart =
                getCheckoutCart(userId);

        StockRequestDTO stockRequest =
                createStockRequest(cart);

        Long orderId =
                persistenceService
                        .createPendingOrder(userId);

        boolean stockReserved = false;

        try {
            StockReservationResponseDTO reservation =
                    reserveStock(stockRequest);

            stockReserved = true;

            OrderResponseDTO pendingOrder =
                    persistenceService.preparePendingOrder(
                            orderId,
                            reservation
                    );

            PaymentResponseDTO payment = processPayment(
                    orderId,
                    userId,
                    pendingOrder.getTotalAmount()
            );

            return new CheckoutResponseDTO(
                    pendingOrder.getId(),
                    pendingOrder.getTotalAmount(),
                    pendingOrder.getStatus(),
                    payment.getStatus(),
                    payment.getCurrency(),
                    payment.getRazorpayKeyId(),
                    payment.getRazorpayOrderId()
            );

        } catch (RuntimeException checkoutFailure) {
            if (stockReserved) {
                try {
                    restoreStock(stockRequest);

                } catch (RuntimeException compensationFailure) {

                    markCompensationPendingSafely(
                            orderId
                    );

                    throw new CompensationFailedException(
                            "Checkout failed and stock restoration is pending",
                            compensationFailure
                    );
                }
            }

            markOrderFailedSafely(orderId);

            throw checkoutFailure;
        }
    }

    private void validateUser(Long userId) {

        try {
            ApiResponse<UserSummaryDTO> response =
                    userClient.getUserById(userId);

            if (response == null
                    || !response.isSuccess()
                    || response.getData() == null) {

                throw new ExternalServiceException(
                        "Invalid response received from User Service"
                );
            }

        } catch (FeignException.NotFound exception) {

            throw new ResourceNotFoundException(
                    "User not found with id: "
                            + userId
            );

        } catch (FeignException exception) {

            throw new ExternalServiceException(
                    "User Service is currently unavailable"
            );
        }
    }

    private CheckoutCartDTO getCheckoutCart(
            Long userId
    ) {

        try {
            ApiResponse<CheckoutCartDTO> response =
                    cartClient.getCheckoutCart(
                            userId
                    );

            if (response == null
                    || !response.isSuccess()
                    || response.getData() == null) {

                throw new ExternalServiceException(
                        "Invalid response received from Cart Service"
                );
            }

            CheckoutCartDTO cart =
                    response.getData();

            if (cart.getItems() == null
                    || cart.getItems().isEmpty()) {

                throw new CartEmptyException(
                        "Cart is empty"
                );
            }

            return cart;

        } catch (FeignException.BadRequest exception) {

            throw new CartEmptyException(
                    "Cart is empty. Add items before checkout"
            );

        } catch (FeignException exception) {

            throw new ExternalServiceException(
                    "Cart Service is currently unavailable"
            );
        }
    }

    private StockRequestDTO createStockRequest(
            CheckoutCartDTO cart
    ) {

        List<StockItemRequestDTO> items =
                cart.getItems()
                        .stream()
                        .map(cartItem ->
                                new StockItemRequestDTO(
                                        cartItem.getProductId(),
                                        cartItem.getQuantity()
                                )
                        )
                        .toList();

        return new StockRequestDTO(items);
    }

    private StockReservationResponseDTO reserveStock(
            StockRequestDTO stockRequest
    ) {

        try {
            ApiResponse<StockReservationResponseDTO> response =
                    productStockClient
                            .reserveStock(
                                    stockRequest
                            );

            if (response == null
                    || !response.isSuccess()
                    || response.getData() == null) {

                throw new ExternalServiceException(
                        "Invalid response received from Product Service"
                );
            }

            return response.getData();

        } catch (FeignException.NotFound exception) {

            throw new ResourceNotFoundException(
                    "One or more Products are unavailable"
            );

        } catch (FeignException.Conflict exception) {

            throw new InsufficientStockException(
                    "Insufficient Product stock"
            );

        } catch (FeignException exception) {

            throw new ExternalServiceException(
                    "Product Service is currently unavailable"
            );
        }
    }

    private void restoreStock(
            StockRequestDTO stockRequest
    ) {

        try {
            productStockClient.restoreStock(
                    stockRequest
            );

        } catch (FeignException exception) {

            throw new ExternalServiceException(
                    "Product stock could not be restored"
            );
        }
    }

    private PaymentResponseDTO processPayment(
            Long orderId,
            Long userId,
            BigDecimal amount
    ) {

        PaymentRequestDTO request =
                new PaymentRequestDTO(
                        orderId,
                        userId,
                        amount
                );

        try {
            PaymentResponseDTO response =
                    paymentClient.processPayment(request);

            if (response == null
                    || response.getRazorpayOrderId() == null
                    || response.getRazorpayKeyId() == null) {

                throw new ExternalServiceException(
                        "Invalid response received from Payment Service"
                );
            }

            return response;

        } catch (FeignException.Conflict exception) {

            throw new ExternalServiceException(
                    "Payment already exists for Order: "
                            + orderId
            );

        } catch (FeignException.BadRequest exception) {

            throw new ExternalServiceException(
                    "Payment request was rejected"
            );

        } catch (FeignException exception) {

            throw new ExternalServiceException(
                    "Payment Service is currently unavailable"
            );
        }
    }

    private void markOrderFailedSafely(
            Long orderId
    ) {

        try {
            persistenceService.markOrderFailed(
                    orderId
            );

        } catch (RuntimeException exception) {

            log.error(
                    "Could not mark Order {} as FAILED",
                    orderId,
                    exception
            );
        }
    }

    private void markCompensationPendingSafely(
            Long orderId
    ) {

        try {
            persistenceService
                    .markCompensationPending(
                            orderId
                    );

        } catch (RuntimeException exception) {

            log.error(
                    "Could not mark Order {} as COMPENSATION_PENDING",
                    orderId,
                    exception
            );
        }
    }
}
