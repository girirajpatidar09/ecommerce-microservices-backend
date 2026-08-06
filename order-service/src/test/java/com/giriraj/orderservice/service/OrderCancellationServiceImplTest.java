package com.giriraj.orderservice.service;

import com.giriraj.orderservice.client.PaymentClient;
import com.giriraj.orderservice.client.ProductStockClient;
import com.giriraj.orderservice.dto.remote.StockItemRequestDTO;
import com.giriraj.orderservice.dto.remote.StockRequestDTO;
import com.giriraj.orderservice.exception.ExternalServiceException;
import com.giriraj.orderservice.exception.InvalidOrderStateException;
import com.giriraj.orderservice.exception.ResourceNotFoundException;
import com.giriraj.orderservice.service.impl.OrderCancellationServiceImpl;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderCancellationServiceImplTest {

    @Mock
    private ProductStockClient productStockClient;

    @Mock
    private PaymentClient paymentClient;

    @Mock
    private OrderPersistenceService persistenceService;

    @InjectMocks
    private OrderCancellationServiceImpl cancellationService;

    private StockRequestDTO stockRequest;

    @BeforeEach
    void setUp() {

        stockRequest = new StockRequestDTO(
                List.of(
                        new StockItemRequestDTO(
                                10L,
                                2
                        )
                )
        );
    }

    @Test
    void cancelOrder_shouldRefundThenRestoreStockThenCompleteCancellation() {

        when(persistenceService.beginCancellation(
                1L,
                100L
        )).thenReturn(stockRequest);

        cancellationService.cancelOrder(
                1L,
                100L
        );

        InOrder inOrder = inOrder(
                persistenceService,
                paymentClient,
                productStockClient
        );

        inOrder.verify(persistenceService)
                .beginCancellation(
                        1L,
                        100L
                );

        inOrder.verify(paymentClient)
                .refundPayment(100L);

        inOrder.verify(productStockClient)
                .restoreStock(stockRequest);

        inOrder.verify(persistenceService)
                .completeCancellation(100L);
    }

    @Test
    void cancelOrder_whenRefundFails_shouldNotRestoreStock() {

        when(persistenceService.beginCancellation(
                1L,
                100L
        )).thenReturn(stockRequest);

        FeignException feignFailure =
                mock(FeignException.class);

        doThrow(feignFailure)
                .when(paymentClient)
                .refundPayment(100L);

        assertThrows(
                ExternalServiceException.class,
                () -> cancellationService.cancelOrder(
                        1L,
                        100L
                )
        );

        verifyNoInteractions(productStockClient);

        verify(persistenceService, never())
                .completeCancellation(anyLong());
    }

    @Test
    void cancelOrder_whenStockRestoreFails_shouldLeaveCancellationPending() {

        when(persistenceService.beginCancellation(
                1L,
                100L
        )).thenReturn(stockRequest);

        FeignException feignFailure =
                mock(FeignException.class);

        doThrow(feignFailure)
                .when(productStockClient)
                .restoreStock(stockRequest);

        assertThrows(
                ExternalServiceException.class,
                () -> cancellationService.cancelOrder(
                        1L,
                        100L
                )
        );

        verify(paymentClient)
                .refundPayment(100L);

        verify(persistenceService, never())
                .completeCancellation(anyLong());
    }

    @Test
    void cancelOrder_whenOrderStateIsInvalid_shouldNotCallProductService() {

        when(persistenceService.beginCancellation(
                1L,
                100L
        )).thenThrow(
                new InvalidOrderStateException(
                        "Shipped Orders cannot be cancelled"
                )
        );

        assertThrows(
                InvalidOrderStateException.class,
                () -> cancellationService.cancelOrder(
                        1L,
                        100L
                )
        );

        verifyNoInteractions(
                paymentClient,
                productStockClient
        );

        verify(persistenceService, never())
                .completeCancellation(anyLong());
    }

    @Test
    void cancelOrder_whenOrderBelongsToAnotherUser_shouldNotRestoreStock() {

        when(persistenceService.beginCancellation(
                2L,
                100L
        )).thenThrow(
                new ResourceNotFoundException(
                        "Order not found"
                )
        );

        assertThrows(
                ResourceNotFoundException.class,
                () -> cancellationService.cancelOrder(
                        2L,
                        100L
                )
        );

        verifyNoInteractions(
                paymentClient,
                productStockClient
        );
    }
}
