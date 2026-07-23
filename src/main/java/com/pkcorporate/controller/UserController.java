package com.pkcorporate.controller;

import com.pkcorporate.dto.request.CreateUserRequest;
import com.pkcorporate.dto.response.ApiResponse;
import com.pkcorporate.dto.response.UserResponse;
import com.pkcorporate.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Admin-only user management endpoints")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    @PostMapping
    @Operation(summary = "Create a new user (AGENT, ACCOUNTANT, or DESIGNER)")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        UserResponse user = userService.createUser(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("User account created successfully", user));
    }

    @GetMapping
    @Operation(summary = "Get all users, optionally filtered by role")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsers(
            @RequestParam(required = false) String role) {
        List<UserResponse> users = role != null
                ? userService.getUsersByRole(role)
                : userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate a user account")
    public ResponseEntity<ApiResponse<UserResponse>> deactivateUser(@PathVariable UUID id) {
        UserResponse user = userService.deactivateUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deactivated", user));
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Activate a user account")
    public ResponseEntity<ApiResponse<UserResponse>> activateUser(@PathVariable UUID id) {
        UserResponse user = userService.activateUser(id);
        return ResponseEntity.ok(ApiResponse.success("User activated", user));
    }
}
