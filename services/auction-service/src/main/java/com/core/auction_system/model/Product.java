package com.core.auction_system.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private Double minBid;

    @Column(nullable = false)
    private Double maxBid;

    private Double currentBid = 0.0;

    private Boolean frozen = false;
    private Boolean sold = false;

    private LocalDateTime endTime;

    // Seller and buyer reference by id only to keep this service decoupled from auth service
    @Column(name = "seller_id")
    private Integer sellerId;

    // Buyer reference (set when sold)
    @Column(name = "buyer_id")
    private Integer buyerId;

    // Lombok will generate constructors, getters, setters

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return Objects.equals(id, product.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
