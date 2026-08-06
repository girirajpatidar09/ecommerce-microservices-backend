package com.giriraj.productservice.service;

import com.giriraj.productservice.dto.ProductImageDTO;
import com.giriraj.productservice.dto.ProductRequestDTO;
import com.giriraj.productservice.dto.ProductResponseDTO;
import com.giriraj.productservice.entity.Product;
import com.giriraj.productservice.entity.ProductImage;
import com.giriraj.productservice.enums.Category;
import com.giriraj.productservice.exception.ResourceNotFoundException;
import com.giriraj.productservice.mapper.ProductMapper;
import com.giriraj.productservice.repository.ProductRepository;
import com.giriraj.productservice.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    private ProductImageDTO imageDTO;
    private ProductRequestDTO request;
    private Product product;
    private ProductResponseDTO response;

    @BeforeEach
    void setUp() {

        imageDTO = new ProductImageDTO(
                "https://example.com/product.jpg"
        );

        request = new ProductRequestDTO(
                "Wireless Headphones",
                "Bluetooth headphones",
                new BigDecimal("2499.99"),
                20,
                Category.ELECTRONICS,
                List.of(imageDTO)
        );

        product = new Product();
        product.setId(1L);
        product.setName("Wireless Headphones");
        product.setDescription("Bluetooth headphones");
        product.setPrice(new BigDecimal("2499.99"));
        product.setStockQuantity(20);
        product.setCategory(Category.ELECTRONICS);

        response = new ProductResponseDTO();
        response.setId(1L);
        response.setName("Wireless Headphones");
        response.setActive(true);
    }

    @Test
    void createProduct_shouldSetImageParentAndSave() {

        ProductImage image = new ProductImage();
        image.setImageUrl(imageDTO.getImageUrl());

        when(productMapper.toEntity(request))
                .thenReturn(product);

        when(productMapper.toImageEntity(imageDTO))
                .thenReturn(image);

        when(productRepository.save(product))
                .thenReturn(product);

        when(productMapper.toResponseDTO(product))
                .thenReturn(response);

        ProductResponseDTO result =
                productService.createProduct(request);

        assertSame(response, result);
        assertEquals(1, product.getImages().size());
        assertSame(
                product,
                product.getImages().get(0).getProduct()
        );

        verify(productRepository).save(product);
    }

    @Test
    void getProductById_whenProductExists_shouldReturnProduct() {

        when(productRepository.findByIdAndActiveTrue(1L))
                .thenReturn(Optional.of(product));

        when(productMapper.toResponseDTO(product))
                .thenReturn(response);

        ProductResponseDTO result =
                productService.getProductById(1L);

        assertSame(response, result);
    }

    @Test
    void getProductById_whenProductDoesNotExist_shouldThrowNotFound() {

        when(productRepository.findByIdAndActiveTrue(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> productService.getProductById(99L)
        );
    }

    @Test
    void deleteProduct_shouldDeactivateInsteadOfDeleting() {

        when(productRepository.findByIdAndActiveTrue(1L))
                .thenReturn(Optional.of(product));

        when(productRepository.save(product))
                .thenReturn(product);

        productService.deleteProduct(1L);

        assertFalse(product.isActive());

        verify(productRepository).save(product);
        verify(productRepository, never())
                .delete(any(Product.class));
    }

    @Test
    void updateProduct_shouldReplaceImages() {

        ProductImage oldImage = new ProductImage();
        oldImage.setImageUrl(
                "https://example.com/old.jpg"
        );
        product.addImage(oldImage);

        ProductImage newImage = new ProductImage();
        newImage.setImageUrl(imageDTO.getImageUrl());

        when(productRepository.findByIdAndActiveTrue(1L))
                .thenReturn(Optional.of(product));

        when(productMapper.toImageEntity(imageDTO))
                .thenReturn(newImage);

        when(productRepository.save(product))
                .thenReturn(product);

        when(productMapper.toResponseDTO(product))
                .thenReturn(response);

        ProductResponseDTO result =
                productService.updateProduct(1L, request);

        assertSame(response, result);
        assertEquals(1, product.getImages().size());
        assertSame(newImage, product.getImages().get(0));
        assertSame(product, newImage.getProduct());
        assertFalse(product.getImages().contains(oldImage));

        verify(productRepository).save(product);
    }

    @Test
    void searchProducts_shouldTrimKeyword() {

        when(productRepository.searchActiveProducts(
                "headphone"
        )).thenReturn(List.of(product));

        when(productMapper.toResponseDTO(product))
                .thenReturn(response);

        List<ProductResponseDTO> result =
                productService.searchProducts(
                        "  headphone  "
                );

        assertEquals(1, result.size());
        assertSame(response, result.get(0));

        verify(productRepository)
                .searchActiveProducts("headphone");
    }
}