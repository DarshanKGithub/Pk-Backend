package com.pkcorporate.service;

import com.pkcorporate.entity.Notification;
import com.pkcorporate.entity.Order;
import com.pkcorporate.entity.User;
import com.pkcorporate.enums.Role;
import com.pkcorporate.repository.NotificationRepository;
import com.pkcorporate.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Async
    @Transactional
    public void notifyOrderCreated(Order order) {
        // Notify all admins and accountants
        List<User> admins = userRepository.findByRole(Role.ADMIN);
        List<User> accountants = userRepository.findByRole(Role.ACCOUNTANT);

        String msg = "New order " + order.getOrderNumber() + " from " +
                     order.getCustomer().getName() + " — ₹" + order.getTotalAmount();

        admins.forEach(u -> createNotification(u, "New Order Created", msg, "ORDER",
                order.getId().toString(), "/admin/orders"));
        accountants.forEach(u -> createNotification(u, "New Order — Awaiting Payment",
                "Order " + order.getOrderNumber() + " needs payment verification", "ORDER",
                order.getId().toString(), "/accountant/payments"));
    }

    @Async
    @Transactional
    public void notifyOrderStatusChange(Order order) {
        String title = "Order Status Updated";
        String msg = "Order " + order.getOrderNumber() + " is now: " + order.getStatus().name();

        // Notify agent
        createNotification(order.getAgent(), title, msg, "ORDER",
                order.getId().toString(), "/agent/orders");

        // Notify designer if design stage
        if (order.getDesigner() != null) {
            createNotification(order.getDesigner(), title, msg, "ORDER",
                    order.getId().toString(), "/designer/orders");
        }
    }

    @Async
    @Transactional
    public void notifyPaymentVerified(Order order) {
        String msg = "Payment verified for order " + order.getOrderNumber() +
                     " — Moving to design stage";

        createNotification(order.getAgent(), "Payment Verified", msg, "PAYMENT",
                order.getId().toString(), "/agent/orders");

        List<User> designers = userRepository.findByRole(Role.DESIGNER);
        designers.forEach(d -> createNotification(d, "New Design Assignment",
                "Order " + order.getOrderNumber() + " is ready for design", "ORDER",
                order.getId().toString(), "/designer/dashboard"));
    }

    public List<Notification> getUserNotifications(UUID userId) {
        // Cap at the 100 most recent — bounds memory + payload on the free tier.
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 100));
    }

    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markAllRead(UUID userId) {
        notificationRepository.markAllAsRead(userId);
    }

    private void createNotification(User user, String title, String message,
                                     String type, String referenceId, String actionUrl) {
        Notification notif = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .referenceId(referenceId)
                .actionUrl(actionUrl)
                .build();
        notificationRepository.save(notif);
    }
}
