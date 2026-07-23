package com.pkcorporate.controller;

import com.pkcorporate.dto.request.CreateCustomerRequest;
import com.pkcorporate.dto.response.ApiResponse;
import com.pkcorporate.dto.response.CustomerResponse;
import com.pkcorporate.entity.User;
import com.pkcorporate.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customer Management", description = "Customer endpoints for agents and admin")
@PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @Operation(summary = "Create a new customer")
    public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request,
            @AuthenticationPrincipal User user) {
        CustomerResponse customer = customerService.createCustomer(request, user.getId());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Customer registered successfully", customer));
    }

    @GetMapping
    @Operation(summary = "Get list of customers with search and pagination")
    public ResponseEntity<ApiResponse<Page<CustomerResponse>>> getCustomers(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<CustomerResponse> customers = customerService.getCustomers(
                user.getId(),
                user.getRole().name(),
                query,
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        );
        return ResponseEntity.ok(ApiResponse.success(customers));
    }
}
