package org.example.catalog.service;

import org.example.catalog.exception.ProductNotFoundException;
import org.example.catalog.mapper.Mapper;
import org.example.catalog.model.PageResponse;
import org.example.catalog.model.Product;
import org.example.catalog.model.ProductRequest;
import org.example.catalog.model.ProductResponse;
import org.example.catalog.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Service
public class ProductService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> SORTABLE_FIELDS = Set.of("name", "price", "createdAt");

    @Autowired
    private ProductRepository productRepository;

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {

        Product product = Mapper.toProduct(request);
        product = productRepository.save(product);
        return Mapper.fromProduct(product);
    }

    // reads are much more frequent than writes, so single product lookups are cached
    @Cacheable(value = "products", key = "#id")
    @Transactional(readOnly = true)
    public ProductResponse getProduct(UUID id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        return Mapper.fromProduct(product);
    }

    @CacheEvict(value = "products", key = "#id")
    @Transactional
    public ProductResponse updateProduct(UUID id, ProductRequest request) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        Mapper.updateProduct(product, request);
        product = productRepository.saveAndFlush(product);
        return Mapper.fromProduct(product);
    }

    @CacheEvict(value = "products", key = "#id")
    @Transactional
    public void deleteProduct(UUID id) {

        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
        productRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> searchProducts(String category, BigDecimal minPrice, BigDecimal maxPrice,
                                                        int page, int size, String sortBy, String direction) {

        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("minPrice must not be greater than maxPrice");
        }
        if (page < 0) {
            throw new IllegalArgumentException("page must be 0 or greater");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        if (!SORTABLE_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException("sortBy must be one of " + SORTABLE_FIELDS);
        }

        String categoryFilter = (category == null || category.isBlank()) ? null : category.trim();

        // id as second sort key keeps the order stable between pages when values are equal
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy).and(Sort.by("id"));

        Page<Product> result = productRepository.search(categoryFilter, minPrice, maxPrice,
                PageRequest.of(page, size, sort));

        return Mapper.fromPage(result);
    }
}
