package com.giriraj.orderservice.service.impl;

import com.giriraj.orderservice.client.PaymentClient;
import com.giriraj.orderservice.client.ProductStockClient;
import com.giriraj.orderservice.dto.remote.StockRequestDTO;
import com.giriraj.orderservice.exception.ExternalServiceException;
import com.giriraj.orderservice.service.OrderCancellationService;
import com.giriraj.orderservice.service.OrderPersistenceService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderCancellationServiceImpl
        implements OrderCancellationService {

    private final ProductStockClient productStockClient;
    private final PaymentClient paymentClient;
    private final OrderPersistenceService persistenceService;

    @Override
    public void cancelOrder(
            Long userId,
            Long orderId
    ) {

        /*
         * Atomically validates ownership/status and changes
         * status to CANCELLATION_PENDING.
         */
        StockRequestDTO stockRequest =
                persistenceService.beginCancellation(
                        userId,
                        orderId
                );

        try {

            paymentClient.refundPayment(orderId);

        } catch (FeignException exception) {

            throw new ExternalServiceException(
                    "Order cancellation is pending because Payment refund failed"
            );
        }

        try {

            productStockClient.restoreStock(stockRequest);

        } catch (FeignException exception) {

            throw new ExternalServiceException(
                    "Payment was refunded, but Order cancellation "
                            + "is pending because Product stock "
                            + "could not be restored"
            );
        }

        persistenceService.completeCancellation(orderId);
    }
}
