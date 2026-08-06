package com.giriraj.orderservice.service.impl;

import com.giriraj.orderservice.client.CartClient;
import com.giriraj.orderservice.client.PaymentClient;
import com.giriraj.orderservice.client.UserClient;
import com.giriraj.orderservice.dto.ApiResponse;
import com.giriraj.orderservice.dto.OrderResponseDTO;
import com.giriraj.orderservice.dto.PaymentVerificationRequestDTO;
import com.giriraj.orderservice.dto.remote.PaymentResponseDTO;
import com.giriraj.orderservice.dto.remote.UserSummaryDTO;
import com.giriraj.orderservice.enums.OrderStatus;
import com.giriraj.orderservice.event.OrderConfirmedEvent;
import com.giriraj.orderservice.event.OrderEventPublisher;
import com.giriraj.orderservice.exception.ExternalServiceException;
import com.giriraj.orderservice.exception.InvalidOrderStateException;
import com.giriraj.orderservice.service.OrderPaymentService;
import com.giriraj.orderservice.service.OrderPersistenceService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderPaymentServiceImpl
        implements OrderPaymentService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    OrderPaymentServiceImpl.class
            );

    private final PaymentClient paymentClient;
    private final CartClient cartClient;
    private final UserClient userClient;
    private final OrderEventPublisher orderEventPublisher;
    private final OrderPersistenceService persistenceService;

    @Override
    public OrderResponseDTO verifyPayment(
            Long userId,
            PaymentVerificationRequestDTO request
    ) {

        OrderResponseDTO order =
                persistenceService.getOrderById(
                        userId,
                        request.getOrderId()
                );

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException(
                    "Only pending Order payment can be verified"
            );
        }

        PaymentResponseDTO payment =
                verifyWithPaymentService(request);

        if (payment == null
                || payment.getOrderId() == null
                || !request.getOrderId()
                .equals(payment.getOrderId())
                || !"SUCCESS".equals(payment.getStatus())) {

            throw new InvalidOrderStateException(
                    "Payment could not be verified"
            );
        }

        OrderResponseDTO confirmedOrder =
                persistenceService.confirmPaidOrder(
                        userId,
                        request.getOrderId()
                );

        clearCartBestEffort(
                userId,
                request.getOrderId()
        );

        publishOrderConfirmedBestEffort(
                userId,
                confirmedOrder
        );

        return confirmedOrder;
    }

    private PaymentResponseDTO verifyWithPaymentService(
            PaymentVerificationRequestDTO request
    ) {

        try {
            return paymentClient.verifyPayment(request);

        } catch (FeignException.BadRequest
                 | FeignException.NotFound
                 | FeignException.Conflict exception) {

            throw new InvalidOrderStateException(
                    "Payment verification failed"
            );

        } catch (FeignException exception) {

            throw new ExternalServiceException(
                    "Payment Service is currently unavailable"
            );
        }
    }

    private void clearCartBestEffort(
            Long userId,
            Long orderId
    ) {

        try {
            cartClient.clearCheckoutCart(userId);

        } catch (FeignException exception) {

            log.error(
                    "Order {} confirmed but Cart cleanup failed for User {}",
                    orderId,
                    userId,
                    exception
            );
        }
    }

    private void publishOrderConfirmedBestEffort(
            Long userId,
            OrderResponseDTO order
    ) {

        try {
            ApiResponse<UserSummaryDTO> userResponse =
                    userClient.getUserById(userId);

            if (userResponse == null
                    || !userResponse.isSuccess()
                    || userResponse.getData() == null
                    || userResponse.getData().getEmail() == null
                    || userResponse.getData().getEmail().isBlank()) {

                log.warn(
                        "Order {} confirmed but customer email was unavailable",
                        order.getId()
                );

                return;
            }

            OrderConfirmedEvent event =
                    new OrderConfirmedEvent(
                            order.getId(),
                            userId,
                            userResponse.getData().getEmail(),
                            order.getTotalAmount(),
                            LocalDateTime.now()
                    );

            orderEventPublisher.publishOrderConfirmed(event);

        } catch (RuntimeException exception) {

            log.error(
                    "Order {} confirmed but notification event could not be created",
                    order.getId(),
                    exception
            );
        }
    }
}
