package com.giriraj.productservice.mapper;

import com.giriraj.productservice.dto.ProductImageDTO;
import com.giriraj.productservice.dto.ProductRequestDTO;
import com.giriraj.productservice.dto.ProductResponseDTO;
import com.giriraj.productservice.entity.Product;
import com.giriraj.productservice.entity.ProductImage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Product toEntity(ProductRequestDTO request);

    ProductResponseDTO toResponseDTO(Product product);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    ProductImage toImageEntity(ProductImageDTO dto);

    ProductImageDTO toImageDTO(ProductImage image);
}