package com.giriraj.notificationservice.listener;

import com.giriraj.notificationservice.event.OrderConfirmedEvent;
import com.giriraj.notificationservice.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderConfirmedListenerTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private OrderConfirmedListener listener;

    @Test
    void shouldSendEmailForConfirmedOrderEvent() {

        OrderConfirmedEvent event =
                new OrderConfirmedEvent(
                        101L,
                        10L,
                        "customer@example.com",
                        new BigDecimal("1499.00"),
                        LocalDateTime.of(
                                2026,
                                8,
                                11,
                                18,
                                30
                        )
                );

        listener.handleOrderConfirmed(event);

        verify(emailService)
                .sendOrderConfirmation(event);
    }
}
