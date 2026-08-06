package com.giriraj.notificationservice.service;

import com.giriraj.notificationservice.event.OrderConfirmedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    private static final String SENDER_EMAIL =
            "shop@example.com";

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {

        ReflectionTestUtils.setField(
                emailService,
                "senderEmail",
                SENDER_EMAIL
        );
    }

    @Test
    void shouldBuildAndSendOrderConfirmationEmail() {

        OrderConfirmedEvent event = createEvent();

        emailService.sendOrderConfirmation(event);

        ArgumentCaptor<SimpleMailMessage> messageCaptor =
                ArgumentCaptor.forClass(
                        SimpleMailMessage.class
                );

        verify(mailSender).send(
                messageCaptor.capture()
        );

        SimpleMailMessage message =
                messageCaptor.getValue();

        assertEquals(
                SENDER_EMAIL,
                message.getFrom()
        );

        assertArrayEquals(
                new String[]{"customer@example.com"},
                message.getTo()
        );

        assertEquals(
                "Order Confirmed - #101",
                message.getSubject()
        );

        assertTrue(
                message.getText()
                        .contains("Order ID: 101")
        );

        assertTrue(
                message.getText()
                        .contains("Total Amount: INR 1499.00")
        );
    }

    @Test
    void shouldRejectEmailWhenSenderIsNotConfigured() {

        ReflectionTestUtils.setField(
                emailService,
                "senderEmail",
                ""
        );

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> emailService
                                .sendOrderConfirmation(
                                        createEvent()
                                )
                );

        assertEquals(
                "MAIL_USERNAME is not configured",
                exception.getMessage()
        );

        verifyNoInteractions(mailSender);
    }

    private OrderConfirmedEvent createEvent() {

        return new OrderConfirmedEvent(
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
    }
}
