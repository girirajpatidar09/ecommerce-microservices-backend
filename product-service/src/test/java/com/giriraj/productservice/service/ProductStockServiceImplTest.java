package com.giriraj.productservice.service;

import com.giriraj.productservice.dto.stock.StockItemRequestDTO;
import com.giriraj.productservice.dto.stock.StockRequestDTO;
import com.giriraj.productservice.dto.stock.StockReservationResponseDTO;
import com.giriraj.productservice.entity.Product;
import com.giriraj.productservice.entity.ProductImage;
import com.giriraj.productservice.enums.Category;
import com.giriraj.productservice.exception.InsufficientStockException;
import com.giriraj.productservice.exception.ResourceNotFoundException;
import com.giriraj.productservice.repository.ProductRepository;
import com.giriraj.productservice.service.impl.ProductStockServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductStockServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductStockServiceImpl stockService;

    private Product product;

    @BeforeEach
    void setUp() {

        product = createProduct(
                1L,
                "Test Laptop",
                "1000.00",
                10
        );

        ProductImage image =
                new ProductImage();

        image.setImageUrl(
                "https://example.com/laptop.jpg"
        );

        product.addImage(image);
    }

    @Test
    void reserveStock_shouldReduceStockAndReturnSnapshots() {

        when(productRepository.findAllByIdsForUpdate(
                anyCollection()
        )).thenReturn(List.of(product));

        StockRequestDTO request =
                stockRequest(
                        new StockItemRequestDTO(
                                1L,
                                2
                        )
                );

        StockReservationResponseDTO result =
                stockService.reserveStock(request);

        assertEquals(
                8,
                product.getStockQuantity()
        );

        assertEquals(
                1,
                result.getItems().size()
        );

        assertEquals(
                "Test Laptop",
                result.getItems()
                        .get(0)
                        .getProductName()
        );

        assertEquals(
                "https://example.com/laptop.jpg",
                result.getItems()
                        .get(0)
                        .getImageUrl()
        );

        assertEquals(
                0,
                new BigDecimal("2000.00")
                        .compareTo(
                                result.getTotalAmount()
                        )
        );

        assertEquals(
                0,
                new BigDecimal("1000.00")
                        .compareTo(
                                result.getItems()
                                        .get(0)
                                        .getUnitPrice()
                        )
        );
    }

    @Test
    void reserveStock_whenOneItemIsInsufficient_shouldChangeNothing() {

        Product secondProduct =
                createProduct(
                        2L,
                        "Mouse",
                        "500.00",
                        1
                );

        when(productRepository.findAllByIdsForUpdate(
                anyCollection()
        )).thenReturn(
                List.of(
                        product,
                        secondProduct
                )
        );

        StockRequestDTO request =
                stockRequest(
                        new StockItemRequestDTO(
                                1L,
                                2
                        ),
                        new StockItemRequestDTO(
                                2L,
                                3
                        )
                );

        assertThrows(
                InsufficientStockException.class,
                () -> stockService.reserveStock(
                        request
                )
        );

        assertEquals(
                10,
                product.getStockQuantity()
        );

        assertEquals(
                1,
                secondProduct.getStockQuantity()
        );
    }

    @Test
    void reserveStock_whenProductIsInactive_shouldThrow() {

        product.setActive(false);

        when(productRepository.findAllByIdsForUpdate(
                anyCollection()
        )).thenReturn(List.of(product));

        StockRequestDTO request =
                stockRequest(
                        new StockItemRequestDTO(
                                1L,
                                1
                        )
                );

        assertThrows(
                ResourceNotFoundException.class,
                () -> stockService.reserveStock(
                        request
                )
        );

        assertEquals(
                10,
                product.getStockQuantity()
        );
    }

    @Test
    void reserveStock_whenProductDoesNotExist_shouldThrow() {

        when(productRepository.findAllByIdsForUpdate(
                anyCollection()
        )).thenReturn(List.of());

        StockRequestDTO request =
                stockRequest(
                        new StockItemRequestDTO(
                                99L,
                                1
                        )
                );

        assertThrows(
                ResourceNotFoundException.class,
                () -> stockService.reserveStock(
                        request
                )
        );
    }

    @Test
    void restoreStock_shouldRestoreEvenInactiveProduct() {

        product.setActive(false);
        product.setStockQuantity(8);

        when(productRepository.findAllByIdsForUpdate(
                anyCollection()
        )).thenReturn(List.of(product));

        StockRequestDTO request =
                stockRequest(
                        new StockItemRequestDTO(
                                1L,
                                2
                        )
                );

        stockService.restoreStock(request);

        assertEquals(
                10,
                product.getStockQuantity()
        );
    }

    private Product createProduct(
            Long id,
            String name,
            String price,
            Integer stock
    ) {

        Product createdProduct =
                new Product();

        createdProduct.setId(id);
        createdProduct.setName(name);

        createdProduct.setPrice(
                new BigDecimal(price)
        );

        createdProduct.setStockQuantity(
                stock
        );

        createdProduct.setCategory(
                Category.ELECTRONICS
        );

        return createdProduct;
    }

    private StockRequestDTO stockRequest(
            StockItemRequestDTO... items
    ) {

        return new StockRequestDTO(
                List.of(items)
        );
    }
}
