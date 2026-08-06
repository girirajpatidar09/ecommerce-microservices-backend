package com.giriraj.notificationservice.service;

import com.giriraj.notificationservice.event.OrderConfirmedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    EmailService.class
            );

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    public void sendOrderConfirmation(
            OrderConfirmedEvent event
    ) {

        if (senderEmail == null
                || senderEmail.isBlank()) {

            throw new IllegalStateException(
                    "MAIL_USERNAME is not configured"
            );
        }

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setFrom(senderEmail);
        message.setTo(event.getCustomerEmail());

        message.setSubject(
                "Order Confirmed - #"
                        + event.getOrderId()
        );

        message.setText(
                """
                Hello,

                Your order has been confirmed successfully.

                Order ID: %s
                Total Amount: INR %s
                Confirmed At: %s

                Thank you for shopping with us.
                """
                .formatted(
                        event.getOrderId(),
                        event.getTotalAmount(),
                        event.getConfirmedAt()
                )
        );

        mailSender.send(message);

        log.info(
                "Order confirmation email sent for Order {} to {}",
                event.getOrderId(),
                event.getCustomerEmail()
        );
    }
}
