package com.giriraj.orderservice.event;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(
                    OrderEventPublisher.class
            );

    private final KafkaTemplate<Object, Object> kafkaTemplate;

    @Value("${app.kafka.order-confirmed-topic}")
    private String orderConfirmedTopic;

    public void publishOrderConfirmed(
            OrderConfirmedEvent event
    ) {

        String messageKey =
                String.valueOf(event.getOrderId());

        kafkaTemplate.send(
                orderConfirmedTopic,
                messageKey,
                event
        ).whenComplete((result, exception) -> {

            if (exception != null) {

                log.error(
                        "Could not publish Order confirmed event for Order {}",
                        event.getOrderId(),
                        exception
                );

                return;
            }

            log.info(
                    "Order confirmed event published for Order {}",
                    event.getOrderId()
            );
        });
    }
}
