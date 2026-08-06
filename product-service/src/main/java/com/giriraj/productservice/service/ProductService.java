package com.giriraj.productservice.service;

import com.giriraj.productservice.dto.ProductRequestDTO;
import com.giriraj.productservice.dto.ProductResponseDTO;

import java.util.List;

public interface ProductService {

    ProductResponseDTO createProduct(
            ProductRequestDTO request
    );

    List<ProductResponseDTO> getAllProducts();

    ProductResponseDTO getProductById(Long id);

    ProductResponseDTO updateProduct(
            Long id,
            ProductRequestDTO request
    );

    void deleteProduct(Long id);

    List<ProductResponseDTO> searchProducts(
            String keyword
    );
}