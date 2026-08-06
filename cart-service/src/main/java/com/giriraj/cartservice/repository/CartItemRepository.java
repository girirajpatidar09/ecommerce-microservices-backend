package com.giriraj.cartservice.repository;

import com.giriraj.cartservice.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository
        extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByUserIdAndProductId(
            Long userId,
            Long productId
    );

    List<CartItem> findAllByUserIdOrderByCreatedAtAsc(
            Long userId
    );

    void deleteAllByUserId(Long userId);
}