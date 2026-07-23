package com.pkcorporate.controller;

import com.pkcorporate.dto.response.ApiResponse;
import com.pkcorporate.dto.response.DashboardDTOs.AdminStatsResponse;
import com.pkcorporate.dto.response.DashboardDTOs.AgentStatsResponse;
import com.pkcorporate.dto.response.DashboardDTOs.DesignerStatsResponse;
import com.pkcorporate.entity.User;
import com.pkcorporate.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Dashboard statistic endpoints")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get admin dashboard statistics")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getAdminDashboard() {
        AdminStatsResponse stats = dashboardService.getAdminDashboard();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/agent")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @Operation(summary = "Get agent dashboard statistics")
    public ResponseEntity<ApiResponse<AgentStatsResponse>> getAgentDashboard(
            @AuthenticationPrincipal User user) {
        AgentStatsResponse stats = dashboardService.getAgentDashboard(user.getId());
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/designer")
    @PreAuthorize("hasAnyRole('DESIGNER', 'ADMIN')")
    @Operation(summary = "Get designer dashboard statistics")
    public ResponseEntity<ApiResponse<DesignerStatsResponse>> getDesignerDashboard(
            @AuthenticationPrincipal User user) {
        DesignerStatsResponse stats = dashboardService.getDesignerDashboard(user.getId());
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}

