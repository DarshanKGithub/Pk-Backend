package com.pkcorporate.controller;

import com.pkcorporate.dto.request.CreateOrderRequest;
import com.pkcorporate.dto.response.ApiResponse;
import com.pkcorporate.dto.response.OrderResponse;
import com.pkcorporate.entity.User;
import com.pkcorporate.enums.OrderStatus;
import com.pkcorporate.service.OrderService;
import com.pkcorporate.service.CloudinaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order management endpoints")
public class OrderController {

    private final OrderService orderService;
    private final CloudinaryService cloudinaryService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Create new order")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal User user) {
        OrderResponse order = orderService.createOrder(request, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Order created successfully", order));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all orders (Admin)")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<OrderResponse> orders = orderService.getAllOrders(
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @Operation(summary = "Get agent's own orders")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<OrderResponse> orders = orderService.getOrdersByAgent(user.getId(),
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrderById(id)));
    }

    @GetMapping("/track/{token}")
    @Operation(summary = "Track order by token (public)")
    public ResponseEntity<ApiResponse<OrderResponse>> trackOrder(@PathVariable String token) {
        return ResponseEntity.ok(ApiResponse.success(orderService.trackOrder(token)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    @Operation(summary = "Update order status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @PathVariable UUID id,
            @RequestParam OrderStatus status,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(
                "Status updated", orderService.updateOrderStatus(id, status, user.getId())));
    }

    @PostMapping("/{id}/upload-logo")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Upload customer logo files")
    public ResponseEntity<ApiResponse<OrderResponse>> uploadLogo(
            @PathVariable UUID id,
            @RequestParam("files") List<MultipartFile> files,
            @AuthenticationPrincipal User user) throws Exception {
        validateUploadedFiles(files);
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            var result = cloudinaryService.uploadFile(file, "logos/" + id);
            urls.add(result.get("url"));
        }
        OrderResponse updated = orderService.uploadLogos(id, urls);
        return ResponseEntity.ok(ApiResponse.success("Files uploaded", updated));
    }

    @PostMapping("/{id}/upload-references")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    @Operation(summary = "Upload reference design files")
    public ResponseEntity<ApiResponse<OrderResponse>> uploadReferences(
            @PathVariable UUID id,
            @RequestParam("files") List<MultipartFile> files) throws Exception {
        validateUploadedFiles(files);
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            var result = cloudinaryService.uploadFile(file, "references/" + id);
            urls.add(result.get("url"));
        }
        OrderResponse updated = orderService.uploadReferences(id, urls);
        return ResponseEntity.ok(ApiResponse.success("Files uploaded", updated));
    }

    @PatchMapping("/{id}/assign-designer")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign a graphic designer to an order")
    public ResponseEntity<ApiResponse<OrderResponse>> assignDesigner(
            @PathVariable UUID id,
            @RequestParam UUID designerId) {
        OrderResponse order = orderService.assignDesigner(id, designerId);
        return ResponseEntity.ok(ApiResponse.success("Designer assigned successfully", order));
    }

    @PostMapping("/{id}/upload-mockups")
    @PreAuthorize("hasAnyRole('ADMIN', 'DESIGNER')")
    @Operation(summary = "Upload design mockup files")
    public ResponseEntity<ApiResponse<OrderResponse>> uploadMockups(
            @PathVariable UUID id,
            @RequestParam("files") List<MultipartFile> files) throws Exception {
        validateUploadedFiles(files);
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            var result = cloudinaryService.uploadFile(file, "mockups/" + id);
            urls.add(result.get("url"));
        }
        return ResponseEntity.ok(ApiResponse.success("Mockups uploaded",
                orderService.uploadMockups(id, urls)));
    }

    private void validateUploadedFiles(List<MultipartFile> files) {
        if (files == null) return;
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.startsWith("image/") && !contentType.equals("application/pdf"))) {
                throw new com.pkcorporate.exception.BusinessException("Only image files and PDFs are allowed");
            }
            String name = file.getOriginalFilename();
            if (name != null && name.contains(".")) {
                String ext = name.substring(name.lastIndexOf(".") + 1).toLowerCase();
                if (!List.of("jpg", "jpeg", "png", "webp", "gif", "pdf").contains(ext)) {
                    throw new com.pkcorporate.exception.BusinessException("Invalid file extension: " + ext);
                }
            }
            if (file.getSize() > 10 * 1024 * 1024) {
                throw new com.pkcorporate.exception.BusinessException("File size exceeds limit of 10MB");
            }
        }
    }
}
