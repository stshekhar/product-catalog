package org.example.catalog.controller;

import jakarta.validation.Valid;
import org.example.catalog.model.PageResponse;
import org.example.catalog.model.ProductRequest;
import org.example.catalog.model.ProductResponse;
import org.example.catalog.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest body) {

        ProductResponse created = productService.createProduct(body);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    public ProductResponse getProduct(@PathVariable UUID id) {

        return productService.getProduct(id);
    }

    @GetMapping
    public PageResponse<ProductResponse> searchProducts(@RequestParam(required = false) String category,
                                                        @RequestParam(required = false) BigDecimal minPrice,
                                                        @RequestParam(required = false) BigDecimal maxPrice,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size,
                                                        @RequestParam(defaultValue = "name") String sortBy,
                                                        @RequestParam(defaultValue = "asc") String direction) {

        return productService.searchProducts(category, minPrice, maxPrice, page, size, sortBy, direction);
    }

    @PutMapping("/{id}")
    public ProductResponse updateProduct(@PathVariable UUID id, @Valid @RequestBody ProductRequest body) {

        return productService.updateProduct(id, body);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {

        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
