package com.giriraj.paymentservice.service.impl;

import com.giriraj.paymentservice.dto.PaymentRequestDTO;
import com.giriraj.paymentservice.dto.PaymentResponseDTO;
import com.giriraj.paymentservice.dto.PaymentVerificationRequestDTO;
import com.giriraj.paymentservice.entity.Payment;
import com.giriraj.paymentservice.enums.PaymentStatus;
import com.giriraj.paymentservice.exception.InvalidPaymentStateException;
import com.giriraj.paymentservice.exception.PaymentAlreadyExistsException;
import com.giriraj.paymentservice.exception.PaymentGatewayException;
import com.giriraj.paymentservice.exception.PaymentNotFoundException;
import com.giriraj.paymentservice.repository.PaymentRepository;
import com.giriraj.paymentservice.service.PaymentService;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Refund;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final RazorpayClient razorpayClient;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    @Override
    public PaymentResponseDTO processPayment(
            PaymentRequestDTO request
    ) {
        paymentRepository
                .findByOrderId(request.getOrderId())
                .ifPresent(payment -> {
                    throw new PaymentAlreadyExistsException(
                            "Payment already exists for order: "
                                    + request.getOrderId()
                    );
                });

        long amountInPaise =
                convertToPaise(request.getAmount());

        JSONObject orderOptions =
                new JSONObject();

        orderOptions.put(
                "amount",
                amountInPaise
        );

        orderOptions.put(
                "currency",
                "INR"
        );

        orderOptions.put(
                "receipt",
                "order_" + request.getOrderId()
        );

        try {
            Order razorpayOrder =
                    razorpayClient.orders.create(
                            orderOptions
                    );

            Payment payment = new Payment();

            payment.setOrderId(
                    request.getOrderId()
            );

            payment.setUserId(
                    request.getUserId()
            );

            payment.setAmount(
                    request.getAmount()
            );

            payment.setCurrency("INR");

            payment.setRazorpayOrderId(
                    razorpayOrder.get("id")
            );

            payment.setStatus(
                    PaymentStatus.PENDING
            );

            Payment savedPayment =
                    paymentRepository.save(payment);

            return toResponseDTO(savedPayment);

        } catch (RazorpayException exception) {

            throw new PaymentGatewayException(
                    "Unable to create Razorpay Order",
                    exception
            );
        }
    }

    @Override
    public PaymentResponseDTO verifyPayment(
            PaymentVerificationRequestDTO request
    ) {
        Payment payment = paymentRepository
                .findByOrderId(request.getOrderId())
                .orElseThrow(() ->
                        new PaymentNotFoundException(
                                "Payment not found for order: "
                                        + request.getOrderId()
                        )
                );

        if (!request.getRazorpayOrderId()
                .equals(payment.getRazorpayOrderId())) {

            throw new InvalidPaymentStateException(
                    "Razorpay Order ID does not match"
            );
        }

        if (payment.getStatus() == PaymentStatus.SUCCESS) {

            if (request.getRazorpayPaymentId()
                    .equals(payment.getRazorpayPaymentId())) {

                return toResponseDTO(payment);
            }

            throw new InvalidPaymentStateException(
                    "Payment is already completed"
            );
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {

            throw new InvalidPaymentStateException(
                    "Only pending Payments can be verified"
            );
        }

        JSONObject verificationData =
                new JSONObject();

        verificationData.put(
                "razorpay_order_id",
                payment.getRazorpayOrderId()
        );

        verificationData.put(
                "razorpay_payment_id",
                request.getRazorpayPaymentId()
        );

        verificationData.put(
                "razorpay_signature",
                request.getRazorpaySignature()
        );

        try {
            boolean validSignature =
                    Utils.verifyPaymentSignature(
                            verificationData,
                            razorpayKeySecret
                    );

            if (!validSignature) {
                throw new InvalidPaymentStateException(
                        "Invalid Razorpay payment signature"
                );
            }

            com.razorpay.Payment razorpayPayment =
                    razorpayClient.payments.fetch(
                            request.getRazorpayPaymentId()
                    );

            String razorpayStatus =
                    razorpayPayment.get("status");

            String paidRazorpayOrderId =
                    razorpayPayment.get("order_id");

            if (!payment.getRazorpayOrderId()
                    .equals(paidRazorpayOrderId)) {

                throw new InvalidPaymentStateException(
                        "Razorpay Payment belongs to another Order"
                );
            }

            if (!"captured".equalsIgnoreCase(
                    razorpayStatus
            )) {

                throw new InvalidPaymentStateException(
                        "Razorpay Payment is not captured. Current status: "
                                + razorpayStatus
                );
            }

            payment.setRazorpayPaymentId(
                    request.getRazorpayPaymentId()
            );

            payment.setStatus(
                    PaymentStatus.SUCCESS
            );

            Payment savedPayment =
                    paymentRepository.save(payment);

            return toResponseDTO(savedPayment);

        } catch (RazorpayException exception) {

            throw new PaymentGatewayException(
                    "Unable to verify Razorpay Payment",
                    exception
            );
        }
    }

    @Override
    public PaymentResponseDTO refundPayment(
            Long orderId
    ) {
        Payment payment = paymentRepository
                .findByOrderId(orderId)
                .orElseThrow(() ->
                        new PaymentNotFoundException(
                                "Payment not found for order: "
                                        + orderId
                        )
                );

        if (payment.getStatus()
                == PaymentStatus.REFUNDED) {

            throw new InvalidPaymentStateException(
                    "Payment is already refunded for order: "
                            + orderId
            );
        }

        if (payment.getStatus()
                != PaymentStatus.SUCCESS) {

            throw new InvalidPaymentStateException(
                    "Only successful Payments can be refunded. "
                            + "Current status: "
                            + payment.getStatus()
            );
        }

        if (payment.getRazorpayPaymentId() == null
                || payment.getRazorpayPaymentId()
                .isBlank()) {

            throw new InvalidPaymentStateException(
                    "Razorpay Payment ID is missing"
            );
        }

        JSONObject refundOptions =
                new JSONObject();

        refundOptions.put(
                "amount",
                convertToPaise(payment.getAmount())
        );

        try {
            Refund razorpayRefund =
                    razorpayClient.payments.refund(
                            payment.getRazorpayPaymentId(),
                            refundOptions
                    );

            payment.setRazorpayRefundId(
                    razorpayRefund.get("id")
            );

            payment.setStatus(
                    PaymentStatus.REFUNDED
            );

            Payment savedPayment =
                    paymentRepository.save(payment);

            return toResponseDTO(savedPayment);

        } catch (RazorpayException exception) {

            throw new PaymentGatewayException(
                    "Unable to refund Razorpay Payment",
                    exception
            );
        }
    }

    private long convertToPaise(
            BigDecimal amount
    ) {
        try {
            return amount
                    .movePointRight(2)
                    .longValueExact();

        } catch (ArithmeticException exception) {

            throw new InvalidPaymentStateException(
                    "Payment amount must have maximum two decimal places"
            );
        }
    }

    private PaymentResponseDTO toResponseDTO(
            Payment payment
    ) {
        PaymentResponseDTO response =
                new PaymentResponseDTO();

        response.setId(payment.getId());
        response.setOrderId(payment.getOrderId());
        response.setUserId(payment.getUserId());
        response.setAmount(payment.getAmount());
        response.setCurrency(payment.getCurrency());
        response.setRazorpayKeyId(razorpayKeyId);
        response.setRazorpayOrderId(
                payment.getRazorpayOrderId()
        );
        response.setRazorpayPaymentId(
                payment.getRazorpayPaymentId()
        );
        response.setRazorpayRefundId(
                payment.getRazorpayRefundId()
        );
        response.setStatus(payment.getStatus());
        response.setCreatedAt(payment.getCreatedAt());

        return response;
    }
}
