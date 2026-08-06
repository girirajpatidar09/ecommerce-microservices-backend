package com.giriraj.productservice.repository;

import com.giriraj.productservice.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductRepository
        extends JpaRepository<Product, Long> {

    @EntityGraph(attributePaths = "images")
    List<Product> findAllByActiveTrue();

    @EntityGraph(attributePaths = "images")
    Optional<Product> findByIdAndActiveTrue(Long id);

    @EntityGraph(attributePaths = "images")
    @Query("""
            SELECT p
            FROM Product p
            WHERE p.active = true
              AND LOWER(p.name)
                  LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    List<Product> searchActiveProducts(
            @Param("keyword") String keyword
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p
            FROM Product p
            WHERE p.id IN :productIds
            ORDER BY p.id
            """)
    List<Product> findAllByIdsForUpdate(
            @Param("productIds")
            Collection<Long> productIds
    );
}