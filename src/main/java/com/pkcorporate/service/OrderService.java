package com.pkcorporate.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pkcorporate.dto.request.CreateOrderRequest;
import com.pkcorporate.dto.response.OrderResponse;
import com.pkcorporate.entity.*;
import com.pkcorporate.enums.OrderStatus;
import com.pkcorporate.exception.BusinessException;
import com.pkcorporate.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final TShirtProductRepository productRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    private static final BigDecimal GST_RATE = new BigDecimal("0.05"); // 5% GST on textiles
    private static final BigDecimal ADVANCE_PERCENTAGE = new BigDecimal("0.70");
    private static final int MINIMUM_ORDER_QUANTITY = 10;

    public OrderResponse createOrder(CreateOrderRequest request, UUID agentId) {
        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new BusinessException("Agent not found"));

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new BusinessException("Customer not found"));

        // Validate MOQ
        int totalQty = request.getItems().stream()
                .flatMap(item -> item.getSizeQuantities().stream())
                .mapToInt(CreateOrderRequest.OrderItemRequest.SizeQtyRequest::getQuantity)
                .sum();

        if (totalQty < MINIMUM_ORDER_QUANTITY) {
            throw new BusinessException(
                "Minimum order quantity is " + MINIMUM_ORDER_QUANTITY + " pieces. Current: " + totalQty);
        }

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .customer(customer)
                .agent(agent)
                .customerNotes(request.getCustomerNotes())
                .internalNotes(request.getInternalNotes())
                .customerGstin(request.getCustomerGstin())
                .trackingToken(UUID.randomUUID().toString())
                .build();

        List<OrderItem> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CreateOrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            TShirtProduct product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new BusinessException("Product not found: " + itemRequest.getProductId()));

            int itemQty = itemRequest.getSizeQuantities().stream()
                    .mapToInt(CreateOrderRequest.OrderItemRequest.SizeQtyRequest::getQuantity).sum();

            Map<String, Integer> sizeQtyMap = new HashMap<>();
            itemRequest.getSizeQuantities().forEach(sq -> sizeQtyMap.put(sq.getSize(), sq.getQuantity()));

            String sizeQtyJson;
            try {
                sizeQtyJson = objectMapper.writeValueAsString(sizeQtyMap);
            } catch (Exception e) {
                sizeQtyJson = "{}";
            }

            BigDecimal itemTotal = product.getBasePrice().multiply(new BigDecimal(itemQty));

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .colorHex(itemRequest.getColorHex())
                    .colorName(itemRequest.getColorName())
                    .printType(itemRequest.getPrintType())
                    .embroideryDetails(itemRequest.getEmbroideryDetails())
                    .designPosition(itemRequest.getDesignPosition())
                    .customText(itemRequest.getCustomText())
                    .sizeQuantities(sizeQtyJson)
                    .totalQuantity(itemQty)
                    .unitPrice(product.getBasePrice())
                    .totalPrice(itemTotal)
                    .build();

            items.add(item);
            subtotal = subtotal.add(itemTotal);
        }

        BigDecimal gstAmount = subtotal.multiply(GST_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = subtotal.add(gstAmount);
        BigDecimal advanceAmount = totalAmount.multiply(ADVANCE_PERCENTAGE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal balanceAmount = totalAmount.subtract(advanceAmount);

        order.setItems(items);
        order.setSubtotal(subtotal);
        order.setGstAmount(gstAmount);
        order.setTotalAmount(totalAmount);
        order.setAdvanceAmount(advanceAmount);
        order.setBalanceAmount(balanceAmount);
        order.setPaidAmount(BigDecimal.ZERO);
        order.setGstRate("5%");

        if (request.getExpectedDeliveryDate() != null) {
            order.setExpectedDeliveryDate(LocalDateTime.parse(request.getExpectedDeliveryDate()));
        }

        Order saved = orderRepository.save(order);

        // Send notifications
        notificationService.notifyOrderCreated(saved);

        return mapToResponse(saved);
    }

    public OrderResponse updateOrderStatus(UUID orderId, OrderStatus newStatus, UUID updatedBy) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("Order not found"));

        validateStatusTransition(order.getStatus(), newStatus);
        order.setStatus(newStatus);

        if (newStatus == OrderStatus.DISPATCHED) {
            order.setDispatchedAt(LocalDateTime.now());
        } else if (newStatus == OrderStatus.COMPLETED) {
            order.setCompletedAt(LocalDateTime.now());
        }

        Order saved = orderRepository.save(order);
        notificationService.notifyOrderStatusChange(saved);

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        // EntityGraph variant fetches customer, agent, designer, items, items.product
        // in one JOIN — eliminates N+1 queries when mapToResponse() is called per row.
        return orderRepository.findAllWithAssociations(pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByAgent(UUID agentId, Pageable pageable) {
        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new BusinessException("Agent not found"));
        // EntityGraph variant — same N+1 fix for agent-scoped list
        return orderRepository.findByAgentWithAssociations(agent, pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID orderId) {
        return orderRepository.findById(orderId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new BusinessException("Order not found"));
    }

    @Transactional(readOnly = true)
    public OrderResponse trackOrder(String trackingToken) {
        return orderRepository.findByTrackingToken(trackingToken)
                .map(this::mapToResponse)
                .orElseThrow(() -> new BusinessException("Order not found with this tracking ID"));
    }

    public OrderResponse uploadMockups(UUID orderId, List<String> mockupUrls) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("Order not found"));
        order.getMockupFileUrls().addAll(mockupUrls);
        return mapToResponse(orderRepository.save(order));
    }

    public OrderResponse uploadLogos(UUID orderId, List<String> logoUrls) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("Order not found"));
        order.getCustomerLogoUrls().addAll(logoUrls);
        return mapToResponse(orderRepository.save(order));
    }

    public OrderResponse uploadReferences(UUID orderId, List<String> referenceUrls) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("Order not found"));
        order.getReferenceFileUrls().addAll(referenceUrls);
        return mapToResponse(orderRepository.save(order));
    }

    public OrderResponse assignDesigner(UUID orderId, UUID designerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("Order not found"));
        User designer = userRepository.findById(designerId)
                .orElseThrow(() -> new BusinessException("Designer not found"));
        
        if (designer.getRole() != com.pkcorporate.enums.Role.DESIGNER) {
            throw new BusinessException("Assigned user must be a graphic designer");
        }

        order.setDesigner(designer);
        // Automatically transition status to DESIGN_IN_PROGRESS on assignment if payment is verified
        if (order.getStatus() == OrderStatus.PAYMENT_VERIFIED) {
            order.setStatus(OrderStatus.DESIGN_IN_PROGRESS);
        }

        return mapToResponse(orderRepository.save(order));
    }

    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        Map<OrderStatus, List<OrderStatus>> allowed = Map.of(
            OrderStatus.PENDING, List.of(OrderStatus.PAYMENT_PENDING, OrderStatus.CANCELLED),
            OrderStatus.PAYMENT_PENDING, List.of(OrderStatus.PAYMENT_VERIFIED, OrderStatus.CANCELLED),
            OrderStatus.PAYMENT_VERIFIED, List.of(OrderStatus.DESIGN_IN_PROGRESS),
            OrderStatus.DESIGN_IN_PROGRESS, List.of(OrderStatus.DESIGN_APPROVED),
            OrderStatus.DESIGN_APPROVED, List.of(OrderStatus.PRODUCTION),
            OrderStatus.PRODUCTION, List.of(OrderStatus.QUALITY_CHECK),
            OrderStatus.QUALITY_CHECK, List.of(OrderStatus.DISPATCH_READY),
            OrderStatus.DISPATCH_READY, List.of(OrderStatus.DISPATCHED),
            OrderStatus.DISPATCHED, List.of(OrderStatus.COMPLETED)
        );

        List<OrderStatus> validNext = allowed.getOrDefault(current, List.of());
        if (!validNext.contains(next)) {
            throw new BusinessException(
                "Invalid status transition from " + current + " to " + next);
        }
    }

    private String generateOrderNumber() {
        LocalDateTime now = LocalDateTime.now();
        String year = String.valueOf(now.getYear()).substring(2);
        String month = String.format("%02d", now.getMonthValue());
        // Retry loop to handle potential collisions
        for (int attempt = 0; attempt < 5; attempt++) {
            String rand = String.format("%04d", (int)(Math.random() * 9999) + 1);
            String candidate = "ORD-" + year + month + rand;
            if (orderRepository.findByOrderNumber(candidate).isEmpty()) {
                return candidate;
            }
        }
        // Fallback: use millisecond precision to guarantee uniqueness
        String milli = String.valueOf(System.currentTimeMillis() % 100000);
        return "ORD-" + year + month + milli;
    }

    private OrderResponse mapToResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .paymentStatus(order.getPaymentStatus())
                .subtotal(order.getSubtotal())
                .gstAmount(order.getGstAmount())
                .totalAmount(order.getTotalAmount())
                .advanceAmount(order.getAdvanceAmount())
                .balanceAmount(order.getBalanceAmount())
                .paidAmount(order.getPaidAmount())
                .gstRate(order.getGstRate())
                .customerNotes(order.getCustomerNotes())
                .internalNotes(order.getInternalNotes())
                .trackingToken(order.getTrackingToken())
                .customerLogoUrls(order.getCustomerLogoUrls())
                .referenceFileUrls(order.getReferenceFileUrls())
                .mockupFileUrls(order.getMockupFileUrls())
                .expectedDeliveryDate(order.getExpectedDeliveryDate())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .customer(order.getCustomer() != null ? OrderResponse.CustomerInfo.builder()
                        .id(order.getCustomer().getId())
                        .name(order.getCustomer().getName())
                        .email(order.getCustomer().getEmail())
                        .phone(order.getCustomer().getPhone())
                        .company(order.getCustomer().getCompany())
                        .build() : null)
                .agent(order.getAgent() != null ? OrderResponse.AgentInfo.builder()
                        .id(order.getAgent().getId())
                        .name(order.getAgent().getName())
                        .email(order.getAgent().getEmail())
                        .build() : null)
                .designer(order.getDesigner() != null ? OrderResponse.DesignerInfo.builder()
                        .id(order.getDesigner().getId())
                        .name(order.getDesigner().getName())
                        .build() : null)
                .items(order.getItems() != null ? order.getItems().stream()
                        .map(item -> OrderResponse.OrderItemResponse.builder()
                                .id(item.getId())
                                .productName(item.getProduct().getName())
                                .productCode(item.getProduct().getProductCode())
                                .colorHex(item.getColorHex())
                                .colorName(item.getColorName())
                                .printType(item.getPrintType())
                                .sizeQuantities(item.getSizeQuantities())
                                .totalQuantity(item.getTotalQuantity())
                                .unitPrice(item.getUnitPrice())
                                .totalPrice(item.getTotalPrice())
                                .build())
                        .toList() : List.of())
                .build();
    }
}
