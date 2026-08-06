package com.giriraj.notificationservice.listener;

import com.giriraj.notificationservice.event.OrderConfirmedEvent;
import com.giriraj.notificationservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderConfirmedListener {

    private static final Logger log =
            LoggerFactory.getLogger(
                    OrderConfirmedListener.class
            );

    private final EmailService emailService;

    @KafkaListener(
            topics = "${app.kafka.order-confirmed-topic}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleOrderConfirmed(
            OrderConfirmedEvent event
    ) {

        log.info(
                "Received Order confirmed event for Order {}",
                event.getOrderId()
        );

        emailService.sendOrderConfirmation(event);
    }
}
