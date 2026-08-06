package com.giriraj.orderservice.controller;

import com.giriraj.orderservice.dto.ApiResponse;
import com.giriraj.orderservice.dto.CheckoutResponseDTO;
import com.giriraj.orderservice.dto.OrderResponseDTO;
import com.giriraj.orderservice.dto.PaymentVerificationRequestDTO;
import com.giriraj.orderservice.service.OrderCancellationService;
import com.giriraj.orderservice.service.OrderCheckoutService;
import com.giriraj.orderservice.service.OrderPaymentService;
import com.giriraj.orderservice.service.OrderPersistenceService;
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
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderCheckoutService checkoutService;
    private final OrderPaymentService paymentService;
    private final OrderPersistenceService persistenceService;
    private final OrderCancellationService cancellationService;

    @PostMapping
    public ResponseEntity<ApiResponse<CheckoutResponseDTO>>
    placeOrder(
            @RequestHeader("X-User-ID")
            @Positive(message = "User ID must be positive")
            Long userId
    ) {

        CheckoutResponseDTO checkout =
                checkoutService.placeOrder(userId);

        ApiResponse<CheckoutResponseDTO> response =
                new ApiResponse<>(
                        true,
                        "Checkout started successfully",
                        checkout,
                        LocalDateTime.now()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/payments/verify")
    public ResponseEntity<ApiResponse<OrderResponseDTO>>
    verifyPayment(
            @RequestHeader("X-User-ID")
            @Positive(message = "User ID must be positive")
            Long userId,

            @Valid
            @RequestBody
            PaymentVerificationRequestDTO request
    ) {

        OrderResponseDTO confirmedOrder =
                paymentService.verifyPayment(
                        userId,
                        request
                );

        ApiResponse<OrderResponseDTO> response =
                new ApiResponse<>(
                        true,
                        "Payment verified and Order confirmed",
                        confirmedOrder,
                        LocalDateTime.now()
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponseDTO>>>
    getOrdersByUser(
            @RequestHeader("X-User-ID")
            @Positive(message = "User ID must be positive")
            Long userId
    ) {

        List<OrderResponseDTO> orders =
                persistenceService
                        .getOrdersByUser(userId);

        ApiResponse<List<OrderResponseDTO>> response =
                new ApiResponse<>(
                        true,
                        "Orders fetched successfully",
                        orders,
                        LocalDateTime.now()
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponseDTO>>
    getOrderById(
            @RequestHeader("X-User-ID")
            @Positive(message = "User ID must be positive")
            Long userId,

            @PathVariable
            @Positive(message = "Order ID must be positive")
            Long orderId
    ) {

        OrderResponseDTO order =
                persistenceService.getOrderById(
                        userId,
                        orderId
                );

        ApiResponse<OrderResponseDTO> response =
                new ApiResponse<>(
                        true,
                        "Order fetched successfully",
                        order,
                        LocalDateTime.now()
                );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(
            @RequestHeader("X-User-ID")
            @Positive(message = "User ID must be positive")
            Long userId,

            @PathVariable
            @Positive(message = "Order ID must be positive")
            Long orderId
    ) {

        cancellationService.cancelOrder(
                userId,
                orderId
        );

        return ResponseEntity.noContent().build();
    }
}
