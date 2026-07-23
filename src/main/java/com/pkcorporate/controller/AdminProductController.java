package com.pkcorporate.controller;

import com.pkcorporate.dto.request.AdminProductCreateOrUpdateRequest;
import com.pkcorporate.dto.response.ApiResponse;
import com.pkcorporate.dto.response.ProductResponse;
import com.pkcorporate.service.ProductService;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.Set;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/products")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Products", description = "Admin CRUD for catalog products")
public class AdminProductController {

    private final ProductService productService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    private void validateRequest(AdminProductCreateOrUpdateRequest request) {
        Set<ConstraintViolation<AdminProductCreateOrUpdateRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    @Operation(summary = "Get all products (active + inactive) for admin management")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(productService.getAllProducts()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE})


    @Operation(summary = "Create a new product with multiple images")
    public ResponseEntity<ApiResponse<ProductResponse>> create(
            @RequestPart("data") String dataJson,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @RequestParam(value = "primaryImageIndex", required = false) Integer primaryImageIndex
    ) {
        log.info("Received request to create product. Data JSON length: {}, Images count: {}", 
                dataJson != null ? dataJson.length() : 0, 
                images != null ? images.size() : 0);
        
        AdminProductCreateOrUpdateRequest data;
        try {
            data = objectMapper.readValue(dataJson, AdminProductCreateOrUpdateRequest.class);
            log.info("Parsed JSON successfully: productCode={}, name={}", data.productCode(), data.name());
        } catch (Exception e) {
            log.error("Failed to parse product JSON: {}", dataJson, e);
            throw new IllegalArgumentException("Invalid JSON format for product data: " + e.getMessage(), e);
        }

        validateRequest(data);

        ProductResponse created = productService.createProduct(data, images, primaryImageIndex);
        
        log.info("Product created successfully in database. ID: {}, Code: {}, Images saved: {}", 
                created.id(), created.productCode(), created.images() != null ? created.images().size() : 0);
        
        return ResponseEntity.ok(ApiResponse.success(created));
    }

    @PreAuthorize("hasRole('ADMIN')")
@PutMapping(value = "/{id}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE})

    @Operation(summary = "Update an existing product; optionally replace/add images")
    public ResponseEntity<ApiResponse<ProductResponse>> update(
            @PathVariable UUID id,
            @RequestPart("data") String dataJson,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @RequestParam(value = "primaryImageIndex", required = false) Integer primaryImageIndex
    ) {
        log.info("Received request to update product {}. Data JSON length: {}, Images count: {}", 
                id, 
                dataJson != null ? dataJson.length() : 0, 
                images != null ? images.size() : 0);
        
        AdminProductCreateOrUpdateRequest data;
        try {
            data = objectMapper.readValue(dataJson, AdminProductCreateOrUpdateRequest.class);
            log.info("Parsed JSON successfully for update: productCode={}, name={}", data.productCode(), data.name());
        } catch (Exception e) {
            log.error("Failed to parse product JSON for update: {}", dataJson, e);
            throw new IllegalArgumentException("Invalid JSON format for product data: " + e.getMessage(), e);
        }

        validateRequest(data);

        ProductResponse updated = productService.updateProduct(id, data, images, primaryImageIndex);
        
        log.info("Product updated successfully in database. ID: {}, Code: {}, Images saved: {}", 
                updated.id(), updated.productCode(), updated.images() != null ? updated.images().size() : 0);
        
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a product")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/enable")
    @Operation(summary = "Enable a product")
    public ResponseEntity<ApiResponse<ProductResponse>> enable(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(productService.setActive(id, true)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/disable")
    @Operation(summary = "Disable a product")
    public ResponseEntity<ApiResponse<ProductResponse>> disable(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(productService.setActive(id, false)));
    }
}

