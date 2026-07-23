package com.pkcorporate.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

public class DashboardDTOs {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgentStatsResponse {
        private int ordersThisMonth;
        private BigDecimal commissionThisMonth;
        private int totalCustomers;
        private BigDecimal revenueGenerated;
        private List<CommissionTrendPoint> commissionTrend;
        private List<RecentOrderInfo> recentOrders;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class CommissionTrendPoint {
            private String month;
            private BigDecimal commission;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class RecentOrderInfo {
            private String id;
            private String customer;
            private int qty;
            private String amount;
            private String status;
            private String date;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DesignerStatsResponse {
        private int assignedOrders;
        private int mockupsPending;
        private int approvedThisMonth;
        private String avgTurnaround;
        private List<DesignOrderInfo> orders;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class DesignOrderInfo {
            private String id;
            private String dbId;
            private String customer;
            private int qty;
            private String deadline;
            private String status;
            private String priority;
            private String notes;
            private boolean mockupUploaded;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminStatsResponse {
        private BigDecimal totalRevenue;
        private BigDecimal monthRevenue;
        private int activeOrders;
        private BigDecimal pendingPayments;
        private int inProduction;
        private int dispatchReady;
        private int totalCustomers;
        private int completedThisMonth;
        private List<MonthlyRevenuePoint> monthlyRevenue;
        private List<AdminRecentOrder> recentOrders;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class MonthlyRevenuePoint {
            private String month;
            private BigDecimal revenue;
            private int orders;
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class AdminRecentOrder {
            private String id;
            private String customer;
            private int qty;
            private String amount;
            private String status;
            private String agent;
            private String date;
        }
    }
}
