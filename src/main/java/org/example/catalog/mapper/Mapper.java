package org.example.catalog.mapper;

import org.example.catalog.model.PageResponse;
import org.example.catalog.model.Product;
import org.example.catalog.model.ProductRequest;
import org.example.catalog.model.ProductResponse;
import org.springframework.data.domain.Page;

public class Mapper {

    public static Product toProduct(ProductRequest request) {

        Product product = new Product();
        updateProduct(product, request);

        return product;
    }

    public static void updateProduct(Product product, ProductRequest request) {

        product.setName(request.getName().trim());
        product.setDescription(request.getDescription());
        product.setCategory(request.getCategory().trim());
        product.setPrice(request.getPrice());
    }

    public static ProductResponse fromProduct(Product product) {

        ProductResponse response = new ProductResponse();

        response.setId(product.getId().toString());
        response.setName(product.getName());
        response.setDescription(product.getDescription());
        response.setCategory(product.getCategory());
        response.setPrice(product.getPrice());
        response.setCreatedAt(product.getCreatedAt());
        response.setUpdatedAt(product.getUpdatedAt());

        return response;
    }

    public static PageResponse<ProductResponse> fromPage(Page<Product> page) {

        PageResponse<ProductResponse> response = new PageResponse<>();

        response.setContent(page.getContent().stream().map(Mapper::fromProduct).toList());
        response.setPage(page.getNumber());
        response.setSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());

        return response;
    }
}
