package com.pkcorporate.controller;

import com.pkcorporate.dto.request.RecordPaymentRequest;
import com.pkcorporate.dto.response.ApiResponse;
import com.pkcorporate.entity.Order;
import com.pkcorporate.entity.Payment;
import com.pkcorporate.entity.User;
import com.pkcorporate.enums.OrderStatus;
import com.pkcorporate.enums.PaymentStatus;
import com.pkcorporate.repository.OrderRepository;
import com.pkcorporate.repository.PaymentRepository;
import com.pkcorporate.repository.UserRepository;
import com.pkcorporate.service.EmailService;
import com.pkcorporate.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payments", description = "Offline payment logging and verification API endpoints")
public class PaymentController {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    @PostMapping("/record")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'ACCOUNTANT')")
    @Operation(summary = "Record a manual payment entry (Cash, Bank Transfer, Cheque, UPI, Credit)")
    public ResponseEntity<?> recordPayment(
            @Valid @RequestBody RecordPaymentRequest dto,
            @AuthenticationPrincipal User user
    ) {
        try {
            Order order = orderRepository.findById(dto.getOrderId())
                    .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + dto.getOrderId()));

            boolean isAdminOrAccountant = user.getRole().name().equals("ADMIN") || user.getRole().name().equals("ACCOUNTANT");

            Payment payment = Payment.builder()
                    .order(order)
                    .paymentType(dto.getPaymentType().toUpperCase())
                    .amount(dto.getAmount())
                    .paymentMethod(dto.getPaymentMethod().toUpperCase())
                    .transactionId(dto.getTransactionId())
                    .bankName(dto.getBankName())
                    .utrNumber(dto.getUtrNumber())
                    .screenshotUrl(dto.getScreenshotUrl())
                    .notes(dto.getNotes())
                    .verified(isAdminOrAccountant) // Auto-verified if entered by Admin/Accountant
                    .verifiedAt(isAdminOrAccountant ? LocalDateTime.now() : null)
                    .verifiedBy(isAdminOrAccountant ? user : null)
                    .build();

            Payment savedPayment = paymentRepository.save(payment);

            if (savedPayment.isVerified()) {
                syncOrderPaymentDetails(order);
                notificationService.notifyPaymentVerified(order);
                try {
                    emailService.sendPaymentConfirmation(
                            order.getCustomer().getEmail(),
                            order.getCustomer().getName(),
                            order.getOrderNumber(),
                            dto.getAmount().toString()
                    );
                } catch (Exception ex) {
                    log.error("SMTP failure sending payment confirmation for order: {}", order.getOrderNumber(), ex);
                }
            } else {
                // If logged by agent, mark order as PAYMENT_PENDING until verified
                order.setStatus(OrderStatus.PAYMENT_PENDING);
                orderRepository.save(order);
            }

            log.info("Payment of {} recorded successfully for order {} (Verified: {}) by user {}", 
                    dto.getAmount(), order.getOrderNumber(), savedPayment.isVerified(), user.getEmail());

            return ResponseEntity.ok(ApiResponse.success("Payment recorded successfully", Map.of(
                    "paymentId", savedPayment.getId().toString(),
                    "verified", savedPayment.isVerified(),
                    "orderStatus", order.getStatus().name(),
                    "paymentStatus", order.getPaymentStatus().name()
            )));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to record manual payment entry", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Payment recording failed: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/verify")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    @Operation(summary = "Verify / approve a pending payment entry")
    public ResponseEntity<?> verifyPayment(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user
    ) {
        try {
            Payment payment = paymentRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Payment record not found with ID: " + id));

            if (payment.isVerified()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Payment is already verified"));
            }

            payment.setVerified(true);
            payment.setVerifiedAt(LocalDateTime.now());
            payment.setVerifiedBy(user);
            Payment savedPayment = paymentRepository.save(payment);

            Order order = savedPayment.getOrder();
            syncOrderPaymentDetails(order);

            log.info("Payment {} verified successfully by {}", id, user.getEmail());

            notificationService.notifyPaymentVerified(order);
            try {
                emailService.sendPaymentConfirmation(
                        order.getCustomer().getEmail(),
                        order.getCustomer().getName(),
                        order.getOrderNumber(),
                        savedPayment.getAmount().toString()
                );
            } catch (Exception ex) {
                log.error("SMTP failure sending payment confirmation for order: {}", order.getOrderNumber(), ex);
            }

            return ResponseEntity.ok(ApiResponse.success("Payment verified successfully", Map.of(
                    "paymentId", savedPayment.getId().toString(),
                    "orderStatus", order.getStatus().name(),
                    "paymentStatus", order.getPaymentStatus().name()
            )));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to verify payment entry", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Payment verification failed: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Edit a payment record (Admin only)")
    public ResponseEntity<?> updatePayment(
            @PathVariable UUID id,
            @Valid @RequestBody RecordPaymentRequest dto
    ) {
        try {
            Payment payment = paymentRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Payment record not found with ID: " + id));

            payment.setPaymentType(dto.getPaymentType().toUpperCase());
            payment.setAmount(dto.getAmount());
            payment.setPaymentMethod(dto.getPaymentMethod().toUpperCase());
            payment.setTransactionId(dto.getTransactionId());
            payment.setBankName(dto.getBankName());
            payment.setUtrNumber(dto.getUtrNumber());
            payment.setScreenshotUrl(dto.getScreenshotUrl());
            payment.setNotes(dto.getNotes());

            Payment savedPayment = paymentRepository.save(payment);
            syncOrderPaymentDetails(savedPayment.getOrder());

            log.info("Payment {} updated by Admin", id);

            return ResponseEntity.ok(ApiResponse.success("Payment updated successfully", Map.of(
                    "paymentId", savedPayment.getId().toString()
            )));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update payment record", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Payment update failed: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a payment record (Admin only)")
    public ResponseEntity<?> deletePayment(@PathVariable UUID id) {
        try {
            Payment payment = paymentRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Payment record not found with ID: " + id));

            Order order = payment.getOrder();
            paymentRepository.delete(payment);
            syncOrderPaymentDetails(order);

            log.info("Payment {} deleted by Admin", id);

            return ResponseEntity.ok(ApiResponse.success("Payment deleted successfully"));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to delete payment record", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Payment deletion failed: " + e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT', 'AGENT')")
    @Operation(summary = "Retrieve payment transaction logs")
    public ResponseEntity<?> getPayments(@AuthenticationPrincipal User user) {
        try {
            List<Payment> payments;
            if (user.getRole().name().equals("ADMIN") || user.getRole().name().equals("ACCOUNTANT")) {
                payments = paymentRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
            } else {
                payments = paymentRepository.findByAgentId(user.getId());
            }

            // Map entities to custom structure to avoid lazy-loading issues in serialization
            List<?> responseList = payments.stream().map(p -> {
                Map<String, Object> map = new java.util.LinkedHashMap<>();
                map.put("id", p.getId().toString());
                map.put("paymentType", p.getPaymentType());
                map.put("amount", p.getAmount());
                map.put("paymentMethod", p.getPaymentMethod());
                map.put("transactionId", p.getTransactionId() != null ? p.getTransactionId() : "");
                map.put("bankName", p.getBankName() != null ? p.getBankName() : "");
                map.put("utrNumber", p.getUtrNumber() != null ? p.getUtrNumber() : "");
                map.put("screenshotUrl", p.getScreenshotUrl() != null ? p.getScreenshotUrl() : "");
                map.put("notes", p.getNotes() != null ? p.getNotes() : "");
                map.put("verified", p.isVerified());
                map.put("verifiedAt", p.getVerifiedAt() != null ? p.getVerifiedAt().toString() : "");
                map.put("createdAt", p.getCreatedAt().toString());
                
                // Nested structures to match the frontend expectations
                map.put("order", Map.of(
                    "id", p.getOrder().getId().toString(),
                    "orderNumber", p.getOrder().getOrderNumber(),
                    "customer", Map.of("name", p.getOrder().getCustomer().getName())
                ));
                
                if (p.getVerifiedBy() != null) {
                    map.put("verifiedBy", Map.of("name", p.getVerifiedBy().getName()));
                } else {
                    map.put("verifiedBy", null);
                }
                return map;
            }).toList();

            return ResponseEntity.ok(ApiResponse.success(responseList));
        } catch (Exception e) {
            log.error("Failed to retrieve payments", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve payments: " + e.getMessage()));
        }
    }

    private void syncOrderPaymentDetails(Order order) {
        List<Payment> payments = paymentRepository.findByOrderId(order.getId());
        
        BigDecimal totalPaid = payments.stream()
                .filter(Payment::isVerified)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setPaidAmount(totalPaid);
        BigDecimal balance = order.getTotalAmount().subtract(totalPaid);
        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            balance = BigDecimal.ZERO;
        }
        order.setBalanceAmount(balance);

        // Update payment status
        if (totalPaid.compareTo(BigDecimal.ZERO) == 0) {
            order.setPaymentStatus(PaymentStatus.PENDING);
        } else if (totalPaid.compareTo(order.getTotalAmount()) >= 0) {
            order.setPaymentStatus(PaymentStatus.FULLY_PAID);
            if (order.getStatus() == OrderStatus.PENDING || order.getStatus() == OrderStatus.PAYMENT_PENDING || order.getStatus() == OrderStatus.PAYMENT_VERIFIED) {
                // If it is fully paid, let's mark it as COMPLETED or PAYMENT_VERIFIED
                // Standard flow: if order status is just pending payments, set it to PAYMENT_VERIFIED so it moves to design/production
                if (order.getStatus() == OrderStatus.PENDING || order.getStatus() == OrderStatus.PAYMENT_PENDING) {
                    order.setStatus(OrderStatus.PAYMENT_VERIFIED);
                }
            }
        } else if (totalPaid.compareTo(order.getAdvanceAmount()) >= 0) {
            order.setPaymentStatus(PaymentStatus.ADVANCE_PAID);
            if (order.getStatus() == OrderStatus.PENDING || order.getStatus() == OrderStatus.PAYMENT_PENDING) {
                order.setStatus(OrderStatus.PAYMENT_VERIFIED);
            }
        } else {
            order.setPaymentStatus(PaymentStatus.PARTIAL);
        }

        orderRepository.save(order);
    }
}
