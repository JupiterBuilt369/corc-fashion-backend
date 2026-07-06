package com.corc.backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendWelcomeEmail(String to, String name) {
        Context context = new Context();
        context.setVariable("name", name);

        String html = templateEngine.process("welcome-email", context);
        sendHtmlEmail(to, "Welcome to CORC", html);
    }

    @Async
    public void sendPasswordResetEmail(String to, String name, String resetLink) {
        Context context = new Context();
        context.setVariable("name", name);
        context.setVariable("resetLink", resetLink);

        String html = templateEngine.process("password-reset-email", context);
        sendHtmlEmail(to, "Reset Your CORC Password", html);
    }

    @Async
    public void sendNewsletterWelcome(String to) {
        Context context = new Context();
        context.setVariable("email", to);

        String html = templateEngine.process("newsletter-welcome", context);
        sendHtmlEmail(to, "Welcome to the CORC Inner Circle", html);
    }

    @Async
    public void sendOrderConfirmation(String to, String name, String orderId, String total) {
        Context context = new Context();
        context.setVariable("name", name);
        context.setVariable("orderId", orderId);
        context.setVariable("total", total);

        String html = templateEngine.process("order-confirmation", context);
        sendHtmlEmail(to, "Order Confirmed - " + orderId, html);
    }

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent to {} with subject: {}", to, subject);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
