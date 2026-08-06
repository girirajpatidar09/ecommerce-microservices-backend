package com.giriraj.paymentservice.controller;

import com.giriraj.paymentservice.dto.PaymentRequestDTO;
import com.giriraj.paymentservice.dto.PaymentResponseDTO;
import com.giriraj.paymentservice.enums.PaymentStatus;
import com.giriraj.paymentservice.exception.GlobalExceptionHandler;
import com.giriraj.paymentservice.exception.InvalidPaymentStateException;
import com.giriraj.paymentservice.exception.PaymentAlreadyExistsException;
import com.giriraj.paymentservice.exception.PaymentNotFoundException;
import com.giriraj.paymentservice.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@Import(GlobalExceptionHandler.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void processPayment_shouldReturnCreated() throws Exception {

        PaymentResponseDTO response =
                new PaymentResponseDTO(
                        500L,
                        100L,
                        1L,
                        new BigDecimal("1500.00"),
                        PaymentStatus.SUCCESS,
                        LocalDateTime.now()
                );

        when(paymentService.processPayment(
                any(PaymentRequestDTO.class)
        )).thenReturn(response);

        mockMvc.perform(
                        post("/api/payments")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "orderId": 100,
                                          "userId": 1,
                                          "amount": 1500.00
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(500))
                .andExpect(jsonPath("$.orderId").value(100))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(
                        jsonPath("$.status")
                                .value("SUCCESS")
                );
    }

    @Test
    void processPayment_shouldReturnBadRequestForInvalidRequest()
            throws Exception {

        mockMvc.perform(
                        post("/api/payments")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "orderId": null,
                                          "userId": 0,
                                          "amount": 0
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.success").value(false)
                )
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(
                        jsonPath("$.error")
                                .value("Bad Request")
                )
                .andExpect(jsonPath("$.message").exists());

        verifyNoInteractions(paymentService);
    }

    @Test
    void processPayment_shouldReturnConflictForDuplicatePayment()
            throws Exception {

        when(paymentService.processPayment(
                any(PaymentRequestDTO.class)
        )).thenThrow(
                new PaymentAlreadyExistsException(
                        "Payment already exists for order: 100"
                )
        );

        mockMvc.perform(
                        post("/api/payments")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "orderId": 100,
                                          "userId": 1,
                                          "amount": 1500.00
                                        }
                                        """)
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.success").value(false)
                )
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(
                        jsonPath("$.error")
                                .value("Conflict")
                )
                .andExpect(
                        jsonPath("$.message").value(
                                "Payment already exists for order: 100"
                        )
                );
    }

    @Test
    void refundPayment_shouldReturnOk() throws Exception {

        PaymentResponseDTO response =
                new PaymentResponseDTO(
                        500L,
                        100L,
                        1L,
                        new BigDecimal("1500.00"),
                        PaymentStatus.REFUNDED,
                        LocalDateTime.now()
                );

        when(paymentService.refundPayment(100L))
                .thenReturn(response);

        mockMvc.perform(
                        put("/api/payments/orders/100/refund")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(500))
                .andExpect(jsonPath("$.orderId").value(100))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(
                        jsonPath("$.status")
                                .value("REFUNDED")
                );
    }

    @Test
    void refundPayment_shouldReturnNotFound()
            throws Exception {

        when(paymentService.refundPayment(100L))
                .thenThrow(
                        new PaymentNotFoundException(
                                "Payment not found for order: 100"
                        )
                );

        mockMvc.perform(
                        put("/api/payments/orders/100/refund")
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        jsonPath("$.success").value(false)
                )
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(
                        jsonPath("$.error")
                                .value("Not Found")
                )
                .andExpect(
                        jsonPath("$.message").value(
                                "Payment not found for order: 100"
                        )
                );
    }

    @Test
    void refundPayment_shouldReturnConflictForInvalidState()
            throws Exception {

        when(paymentService.refundPayment(100L))
                .thenThrow(
                        new InvalidPaymentStateException(
                                "Payment is already refunded for order: 100"
                        )
                );

        mockMvc.perform(
                        put("/api/payments/orders/100/refund")
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.success").value(false)
                )
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(
                        jsonPath("$.error")
                                .value("Conflict")
                )
                .andExpect(
                        jsonPath("$.message").value(
                                "Payment is already refunded for order: 100"
                        )
                );
    }
}
