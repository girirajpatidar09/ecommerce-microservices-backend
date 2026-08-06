package com.giriraj.productservice.service.impl;

import com.giriraj.productservice.dto.stock.StockItemRequestDTO;
import com.giriraj.productservice.dto.stock.StockRequestDTO;
import com.giriraj.productservice.dto.stock.StockReservationItemDTO;
import com.giriraj.productservice.dto.stock.StockReservationResponseDTO;
import com.giriraj.productservice.entity.Product;
import com.giriraj.productservice.exception.InsufficientStockException;
import com.giriraj.productservice.exception.ResourceNotFoundException;
import com.giriraj.productservice.repository.ProductRepository;
import com.giriraj.productservice.service.ProductStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductStockServiceImpl
        implements ProductStockService {

    private final ProductRepository productRepository;

    @Override
    public StockReservationResponseDTO reserveStock(
            StockRequestDTO request
    ) {

        Map<Long, Integer> requestedQuantities =
                combineQuantities(request);

        Map<Long, Product> products =
                loadProductsForUpdate(
                        requestedQuantities.keySet()
                );

        /*
         * Validate all products before changing stock.
         */
        for (Map.Entry<Long, Integer> entry
                : requestedQuantities.entrySet()) {

            Product product =
                    products.get(entry.getKey());

            if (!product.isActive()) {
                throw new ResourceNotFoundException(
                        "Product is unavailable: "
                                + product.getName()
                );
            }

            if (entry.getValue()
                    > product.getStockQuantity()) {

                throw new InsufficientStockException(
                        "Insufficient stock for product: "
                                + product.getName()
                );
            }
        }

        List<StockReservationItemDTO> responseItems =
                new ArrayList<>();

        BigDecimal totalAmount = BigDecimal.ZERO;

        /*
         * All validation succeeded.
         * Stock can now be reduced.
         */
        for (Map.Entry<Long, Integer> entry
                : requestedQuantities.entrySet()) {

            Product product =
                    products.get(entry.getKey());

            Integer quantity = entry.getValue();

            product.setStockQuantity(
                    product.getStockQuantity() - quantity
            );

            BigDecimal itemTotal =
                    product.getPrice().multiply(
                            BigDecimal.valueOf(quantity)
                    );

            StockReservationItemDTO responseItem =
                    new StockReservationItemDTO(
                            product.getId(),
                            product.getName(),
                            getFirstImageUrl(product),
                            quantity,
                            product.getPrice(),
                            itemTotal
                    );

            responseItems.add(responseItem);

            totalAmount =
                    totalAmount.add(itemTotal);
        }

        return new StockReservationResponseDTO(
                responseItems,
                totalAmount
        );
    }

    @Override
    public void restoreStock(
            StockRequestDTO request
    ) {

        Map<Long, Integer> requestedQuantities =
                combineQuantities(request);

        Map<Long, Product> products =
                loadProductsForUpdate(
                        requestedQuantities.keySet()
                );

        for (Map.Entry<Long, Integer> entry
                : requestedQuantities.entrySet()) {

            Product product =
                    products.get(entry.getKey());

            product.setStockQuantity(
                    product.getStockQuantity()
                            + entry.getValue()
            );
        }
    }

    private Map<Long, Integer> combineQuantities(
            StockRequestDTO request
    ) {

        Map<Long, Integer> quantities =
                new LinkedHashMap<>();

        for (StockItemRequestDTO item
                : request.getItems()) {

            quantities.merge(
                    item.getProductId(),
                    item.getQuantity(),
                    Integer::sum
            );
        }

        return quantities;
    }

    private Map<Long, Product> loadProductsForUpdate(
            Collection<Long> productIds
    ) {

        List<Product> products =
                productRepository
                        .findAllByIdsForUpdate(productIds);

        Map<Long, Product> productMap =
                new HashMap<>();

        for (Product product : products) {
            productMap.put(
                    product.getId(),
                    product
            );
        }

        for (Long productId : productIds) {

            if (!productMap.containsKey(productId)) {

                throw new ResourceNotFoundException(
                        "Product not found with id: "
                                + productId
                );
            }
        }

        return productMap;
    }

    private String getFirstImageUrl(
            Product product
    ) {

        if (product.getImages() == null
                || product.getImages().isEmpty()) {

            return null;
        }

        return product.getImages()
                .get(0)
                .getImageUrl();
    }
}
