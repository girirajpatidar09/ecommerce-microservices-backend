package com.giriraj.productservice.service.impl;

import com.giriraj.productservice.dto.ProductImageDTO;
import com.giriraj.productservice.dto.ProductRequestDTO;
import com.giriraj.productservice.dto.ProductResponseDTO;
import com.giriraj.productservice.entity.Product;
import com.giriraj.productservice.entity.ProductImage;
import com.giriraj.productservice.exception.ResourceNotFoundException;
import com.giriraj.productservice.mapper.ProductMapper;
import com.giriraj.productservice.repository.ProductRepository;
import com.giriraj.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    public ProductResponseDTO createProduct(ProductRequestDTO request) {

        Product product = productMapper.toEntity(request);

        addImages(product, request.getImages());

        Product savedProduct =
                productRepository.save(product);

        return productMapper.toResponseDTO(savedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> getAllProducts() {

        return productRepository.findAllByActiveTrue()
                .stream()
                .map(productMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponseDTO getProductById(Long id) {

        Product product = findActiveProduct(id);

        return productMapper.toResponseDTO(product);
    }

    @Override
    public ProductResponseDTO updateProduct(
            Long id,
            ProductRequestDTO request
    ) {

        Product product = findActiveProduct(id);

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(
                request.getStockQuantity()
        );
        product.setCategory(request.getCategory());

        /*
         * orphanRemoval deletes the old ProductImage rows.
         */
        product.getImages().clear();

        addImages(product, request.getImages());

        Product updatedProduct =
                productRepository.save(product);

        return productMapper.toResponseDTO(updatedProduct);
    }

    @Override
    public void deleteProduct(Long id) {

        Product product = findActiveProduct(id);

        product.setActive(false);

        productRepository.save(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDTO> searchProducts(
            String keyword
    ) {

        String normalizedKeyword =
                keyword == null
                        ? ""
                        : keyword.trim();

        return productRepository
                .searchActiveProducts(normalizedKeyword)
                .stream()
                .map(productMapper::toResponseDTO)
                .toList();
    }

    private Product findActiveProduct(Long id) {

        return productRepository
                .findByIdAndActiveTrue(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found with id: " + id
                        )
                );
    }

    private void addImages(Product product, List<ProductImageDTO> imageDTOs)
    {

        if (imageDTOs == null) {
            return;
        }

        for (ProductImageDTO imageDTO : imageDTOs)
        {

            ProductImage image =productMapper.toImageEntity(imageDTO);

            product.addImage(image);
        }
    }
}