package com.pkcorporate.controller;

import com.pkcorporate.dto.response.ApiResponse;
import com.pkcorporate.dto.response.ProductResponse;
import com.pkcorporate.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/products")
@RequiredArgsConstructor
@Tag(name = "Product Catalog", description = "Product endpoints for catalog browsing")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Get all active products in catalog")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getActiveProducts() {
        List<ProductResponse> products = productService.getActiveProducts();
        return ResponseEntity.ok(ApiResponse.success(products));
    }
}
