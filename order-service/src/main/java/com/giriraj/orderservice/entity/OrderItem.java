package com.giriraj.orderservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Order aur OrderItem same database mein hain,
     * isliye this JPA relationship is correct.
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "order_id",
            nullable = false
    )
    private Order order;

    /*
     * Product Service identifier.
     * This is not a Product entity relationship.
     */
    @Column(name = "product_id", nullable = false)
    private Long productId;

    /*
     * Historical snapshots.
     */
    @Column(
            name = "product_name",
            nullable = false,
            length = 100
    )
    private String productName;

    @Column(
            name = "image_url",
            length = 2048
    )
    private String imageUrl;

    @Column(nullable = false)
    private Integer quantity;

    @Column(
            name = "unit_price",
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal unitPrice;
}