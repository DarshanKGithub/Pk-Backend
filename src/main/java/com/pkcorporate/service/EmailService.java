package com.pkcorporate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Async
    public void sendPasswordResetEmail(String toEmail, String name, String token) {
        String resetLink = frontendUrl + "/auth/reset-password?token=" + token;
        String html = """
                <div style="font-family: Inter, Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <div style="background: #E10600; padding: 24px; border-radius: 12px 12px 0 0;">
                        <h1 style="color: white; margin: 0; font-size: 24px;">PK Corporate ERP</h1>
                    </div>
                    <div style="background: #fff; padding: 32px; border: 1px solid #f0f0f0; border-radius: 0 0 12px 12px;">
                        <h2 style="color: #111; margin-top: 0;">Reset Your Password</h2>
                        <p style="color: #555;">Hello %s,</p>
                        <p style="color: #555;">You requested a password reset. Click the button below to set a new password. This link expires in 1 hour.</p>
                        <div style="text-align: center; margin: 32px 0;">
                            <a href="%s" style="background: #E10600; color: white; padding: 14px 32px; border-radius: 8px; text-decoration: none; font-weight: 600; display: inline-block;">
                                Reset Password
                            </a>
                        </div>
                        <p style="color: #888; font-size: 13px;">If you didn't request this, you can safely ignore this email.</p>
                    </div>
                </div>
                """.formatted(name, resetLink);

        sendHtmlEmail(toEmail, "Reset Your PK Corporate Password", html);
    }

    @Async
    public void sendOrderConfirmation(String toEmail, String customerName, String orderNumber, String trackingToken) {
        String trackingLink = frontendUrl + "/track/" + trackingToken;
        String html = """
                <div style="font-family: Inter, Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <div style="background: #E10600; padding: 24px; border-radius: 12px 12px 0 0;">
                        <h1 style="color: white; margin: 0;">PK Corporate</h1>
                        <p style="color: rgba(255,255,255,0.8); margin: 4px 0 0;">Order Confirmation</p>
                    </div>
                    <div style="background: #fff; padding: 32px; border: 1px solid #f0f0f0; border-radius: 0 0 12px 12px;">
                        <h2 style="color: #111;">Order Placed Successfully!</h2>
                        <p style="color: #555;">Dear %s,</p>
                        <p style="color: #555;">Your order <strong>%s</strong> has been placed successfully.</p>
                        <div style="background: #f9f9f9; border-radius: 8px; padding: 16px; margin: 20px 0;">
                            <p style="margin: 0; color: #555;">Track your order status anytime:</p>
                            <a href="%s" style="color: #E10600; font-weight: 600;">%s</a>
                        </div>
                        <p style="color: #555;">You will receive an advance payment link shortly. Please complete 70%% advance to proceed with production.</p>
                        <p style="color: #888; font-size: 13px;">PK Corporate | Textile Manufacturing ERP</p>
                    </div>
                </div>
                """.formatted(customerName, orderNumber, trackingLink, trackingLink);

        sendHtmlEmail(toEmail, "Order Confirmed: " + orderNumber, html);
    }

    @Async
    public void sendPaymentConfirmation(String toEmail, String customerName, String orderNumber, String amount) {
        String html = """
                <div style="font-family: Inter, Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <div style="background: #E10600; padding: 24px; border-radius: 12px 12px 0 0;">
                        <h1 style="color: white; margin: 0;">PK Corporate</h1>
                    </div>
                    <div style="background: #fff; padding: 32px; border: 1px solid #f0f0f0; border-radius: 0 0 12px 12px;">
                        <h2 style="color: #111;">Payment Received ✓</h2>
                        <p style="color: #555;">Dear %s,</p>
                        <p style="color: #555;">We have received your payment of <strong>₹%s</strong> for order <strong>%s</strong>.</p>
                        <p style="color: #555;">Your order will now move to design & production stage.</p>
                    </div>
                </div>
                """.formatted(customerName, amount, orderNumber);

        sendHtmlEmail(toEmail, "Payment Received: " + orderNumber, html);
    }

    @Async
    public void sendWelcomeEmail(String toEmail, String name, String tempPassword, String role) {
        String html = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <div style="background: #E10600; padding: 24px; border-radius: 12px 12px 0 0; text-align: center;">
                        <h1 style="color: #fff; margin: 0; font-size: 24px;">Welcome to PK Corporate ERP</h1>
                    </div>
                    <div style="background: #fff; padding: 32px; border: 1px solid #f0f0f0; border-radius: 0 0 12px 12px;">
                        <h2 style="color: #111;">Your Account is Ready 🎉</h2>
                        <p style="color: #555;">Dear %s,</p>
                        <p style="color: #555;">An admin has created a <strong>%s</strong> account for you on the PK Corporate ERP system.</p>
                        <div style="background: #f9f9f9; border: 1px solid #eee; border-radius: 8px; padding: 16px; margin: 20px 0;">
                            <p style="margin: 4px 0; color: #333;"><strong>Email:</strong> %s</p>
                            <p style="margin: 4px 0; color: #333;"><strong>Temporary Password:</strong> %s</p>
                        </div>
                        <p style="color: #E10600; font-weight: bold;">Please change your password immediately after logging in.</p>
                        <p style="color: #555;">If you have any questions, contact your administrator.</p>
                    </div>
                </div>
                """.formatted(name, role, toEmail, tempPassword);

        sendHtmlEmail(toEmail, "Your PK Corporate ERP Account", html);
    }

    private void sendHtmlEmail(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, "PK Corporate ERP");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Email sent to {} | Subject: {}", to, subject);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        } catch (Exception e) {
            log.error("Email error: {}", e.getMessage());
        }
    }
}
