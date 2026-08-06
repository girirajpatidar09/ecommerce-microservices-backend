package com.giriraj.productservice.controller;

import com.giriraj.productservice.dto.ApiResponse;
import com.giriraj.productservice.dto.ProductRequestDTO;
import com.giriraj.productservice.dto.ProductResponseDTO;
import com.giriraj.productservice.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponseDTO>>
    createProduct(
            @Valid @RequestBody ProductRequestDTO request
    ) {

        ProductResponseDTO product =
                productService.createProduct(request);

        ApiResponse<ProductResponseDTO> response =
                new ApiResponse<>(
                        true,
                        "Product created successfully",
                        product,
                        LocalDateTime.now()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductResponseDTO>>>
    getAllProducts() {

        List<ProductResponseDTO> products =
                productService.getAllProducts();

        ApiResponse<List<ProductResponseDTO>> response =
                new ApiResponse<>(
                        true,
                        "Products fetched successfully",
                        products,
                        LocalDateTime.now()
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponseDTO>>
    getProductById(
            @PathVariable Long id
    ) {

        ProductResponseDTO product =
                productService.getProductById(id);

        ApiResponse<ProductResponseDTO> response =
                new ApiResponse<>(
                        true,
                        "Product fetched successfully",
                        product,
                        LocalDateTime.now()
                );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponseDTO>>
    updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequestDTO request
    ) {

        ProductResponseDTO product =
                productService.updateProduct(id, request);

        ApiResponse<ProductResponseDTO> response =
                new ApiResponse<>(
                        true,
                        "Product updated successfully",
                        product,
                        LocalDateTime.now()
                );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable Long id
    ) {

        productService.deleteProduct(id);

        ApiResponse<Void> response =
                new ApiResponse<>(
                        true,
                        "Product deactivated successfully",
                        null,
                        LocalDateTime.now()
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ProductResponseDTO>>>
    searchProducts(
            @RequestParam String keyword
    ) {

        List<ProductResponseDTO> products =
                productService.searchProducts(keyword);

        ApiResponse<List<ProductResponseDTO>> response =
                new ApiResponse<>(
                        true,
                        "Products fetched successfully",
                        products,
                        LocalDateTime.now()
                );

        return ResponseEntity.ok(response);
    }
}