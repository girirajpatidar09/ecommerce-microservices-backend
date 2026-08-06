package com.giriraj.paymentservice.service;

import com.giriraj.paymentservice.dto.PaymentRequestDTO;
import com.giriraj.paymentservice.dto.PaymentResponseDTO;
import com.giriraj.paymentservice.entity.Payment;
import com.giriraj.paymentservice.enums.PaymentStatus;
import com.giriraj.paymentservice.exception.InvalidPaymentStateException;
import com.giriraj.paymentservice.exception.PaymentAlreadyExistsException;
import com.giriraj.paymentservice.exception.PaymentNotFoundException;
import com.giriraj.paymentservice.repository.PaymentRepository;
import com.giriraj.paymentservice.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    void processPayment_shouldCreateSuccessfulPayment() {

        PaymentRequestDTO request = new PaymentRequestDTO(
                100L,
                1L,
                new BigDecimal("1500.00")
        );

        LocalDateTime createdAt = LocalDateTime.now();

        when(paymentRepository.findByOrderId(100L))
                .thenReturn(Optional.empty());

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> {

                    Payment payment = invocation.getArgument(0);

                    payment.setId(500L);
                    payment.setCreatedAt(createdAt);

                    return payment;
                });

        PaymentResponseDTO response =
                paymentService.processPayment(request);

        assertEquals(500L, response.getId());
        assertEquals(100L, response.getOrderId());
        assertEquals(1L, response.getUserId());
        assertEquals(
                new BigDecimal("1500.00"),
                response.getAmount()
        );
        assertEquals(
                PaymentStatus.SUCCESS,
                response.getStatus()
        );
        assertEquals(createdAt, response.getCreatedAt());

        ArgumentCaptor<Payment> paymentCaptor =
                ArgumentCaptor.forClass(Payment.class);

        verify(paymentRepository).save(
                paymentCaptor.capture()
        );

        Payment savedPayment = paymentCaptor.getValue();

        assertEquals(100L, savedPayment.getOrderId());
        assertEquals(1L, savedPayment.getUserId());
        assertEquals(
                new BigDecimal("1500.00"),
                savedPayment.getAmount()
        );
        assertEquals(
                PaymentStatus.SUCCESS,
                savedPayment.getStatus()
        );
    }

    @Test
    void processPayment_shouldRejectDuplicatePayment() {

        PaymentRequestDTO request = new PaymentRequestDTO(
                100L,
                1L,
                new BigDecimal("1500.00")
        );

        Payment existingPayment = new Payment();
        existingPayment.setId(500L);
        existingPayment.setOrderId(100L);

        when(paymentRepository.findByOrderId(100L))
                .thenReturn(Optional.of(existingPayment));

        PaymentAlreadyExistsException exception =
                assertThrows(
                        PaymentAlreadyExistsException.class,
                        () -> paymentService.processPayment(request)
                );

        assertEquals(
                "Payment already exists for order: 100",
                exception.getMessage()
        );

        verify(paymentRepository, never())
                .save(any(Payment.class));
    }

    @Test
    void refundPayment_shouldRefundSuccessfulPayment() {

        Payment payment = new Payment();

        payment.setId(500L);
        payment.setOrderId(100L);
        payment.setUserId(1L);
        payment.setAmount(new BigDecimal("1500.00"));
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setCreatedAt(LocalDateTime.now());

        when(paymentRepository.findByOrderId(100L))
                .thenReturn(Optional.of(payment));

        when(paymentRepository.save(payment))
                .thenReturn(payment);

        PaymentResponseDTO response =
                paymentService.refundPayment(100L);

        assertEquals(
                PaymentStatus.REFUNDED,
                response.getStatus()
        );

        assertEquals(
                PaymentStatus.REFUNDED,
                payment.getStatus()
        );

        verify(paymentRepository).save(payment);
    }

    @Test
    void refundPayment_shouldThrowWhenPaymentNotFound() {

        when(paymentRepository.findByOrderId(100L))
                .thenReturn(Optional.empty());

        PaymentNotFoundException exception =
                assertThrows(
                        PaymentNotFoundException.class,
                        () -> paymentService.refundPayment(100L)
                );

        assertEquals(
                "Payment not found for order: 100",
                exception.getMessage()
        );

        verify(paymentRepository, never())
                .save(any(Payment.class));
    }

    @Test
    void refundPayment_shouldRejectAlreadyRefundedPayment() {

        Payment payment = new Payment();

        payment.setOrderId(100L);
        payment.setStatus(PaymentStatus.REFUNDED);

        when(paymentRepository.findByOrderId(100L))
                .thenReturn(Optional.of(payment));

        InvalidPaymentStateException exception =
                assertThrows(
                        InvalidPaymentStateException.class,
                        () -> paymentService.refundPayment(100L)
                );

        assertEquals(
                "Payment is already refunded for order: 100",
                exception.getMessage()
        );

        verify(paymentRepository, never())
                .save(any(Payment.class));
    }

    @Test
    void refundPayment_shouldRejectFailedPayment() {

        Payment payment = new Payment();

        payment.setOrderId(100L);
        payment.setStatus(PaymentStatus.FAILED);

        when(paymentRepository.findByOrderId(100L))
                .thenReturn(Optional.of(payment));

        InvalidPaymentStateException exception =
                assertThrows(
                        InvalidPaymentStateException.class,
                        () -> paymentService.refundPayment(100L)
                );

        assertEquals(
                "Only successful payments can be refunded. "
                        + "Current status: FAILED",
                exception.getMessage()
        );

        verify(paymentRepository, never())
                .save(any(Payment.class));
    }

    @Test
    void refundPayment_shouldRejectPendingPayment() {

        Payment payment = new Payment();

        payment.setOrderId(100L);
        payment.setStatus(PaymentStatus.PENDING);

        when(paymentRepository.findByOrderId(100L))
                .thenReturn(Optional.of(payment));

        InvalidPaymentStateException exception =
                assertThrows(
                        InvalidPaymentStateException.class,
                        () -> paymentService.refundPayment(100L)
                );

        assertEquals(
                "Only successful payments can be refunded. "
                        + "Current status: PENDING",
                exception.getMessage()
        );

        verify(paymentRepository, never())
                .save(any(Payment.class));
    }
}
