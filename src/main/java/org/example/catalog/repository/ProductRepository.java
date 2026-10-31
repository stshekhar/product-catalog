package org.example.catalog.repository;

import org.example.catalog.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    // every filter is optional, a null value means "don't filter on this"
    @Query("select p from Product p "
            + "where (:category is null or p.category = :category) "
            + "and (:minPrice is null or p.price >= :minPrice) "
            + "and (:maxPrice is null or p.price <= :maxPrice)")
    Page<Product> search(@Param("category") String category,
                         @Param("minPrice") BigDecimal minPrice,
                         @Param("maxPrice") BigDecimal maxPrice,
                         Pageable pageable);
}
