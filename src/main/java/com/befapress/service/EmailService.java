package com.befapress.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.name:BEFA Press}")
    private String appName;

    @Async
    public void sendOtpEmail(String to, String name, String otp, String purpose) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(appName + " - " + purpose);

            String htmlContent = buildOtpEmailTemplate(name, otp, purpose);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("OTP email sent successfully to: {}. Code: {}", to, otp);
        } catch (MessagingException e) {
            log.error("Failed to send OTP email to: {}", to, e);
        }
    }

    @Async
    public void sendWelcomeEmail(String to, String name) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Welcome to " + appName);

            String htmlContent = buildWelcomeEmailTemplate(name);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Welcome email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send welcome email to: {}", to, e);
        }
    }

    @Async
    public void sendOpinionApprovalEmail(String to, String name, String opinionTitle, boolean approved) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(appName + " - Opinion " + (approved ? "Approved" : "Review Required"));

            String htmlContent = buildOpinionStatusEmailTemplate(name, opinionTitle, approved);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Opinion status email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send opinion status email to: {}", to, e);
        }
    }

    @Async
    public void sendWelcomeEmailWithPassword(String to, String name, String tempPassword) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(appName + " - Your Account Has Been Created");

            String htmlContent = buildWelcomeWithPasswordTemplate(name, tempPassword);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Welcome email with password sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send welcome email to: {}", to, e);
        }
    }

    @Async
    public void sendPasswordResetEmail(String to, String name, String newPassword) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(appName + " - Password Reset by Administrator");

            String htmlContent = buildPasswordResetTemplate(name, newPassword);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Password reset email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send password reset email to: {}", to, e);
        }
    }

    /**
     * Generic method to send HTML emails (used by SubscriptionService)
     */
    @Async
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("HTML email sent successfully to: {} with subject: {}", to, subject);
        } catch (MessagingException e) {
            log.error("Failed to send HTML email to: {}", to, e);
        }
    }

    @Async
    public void sendStatusChangeEmail(String to, String name, String oldStatus, String newStatus) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(appName + " - Account Status Changed");

            String htmlContent = buildStatusChangeTemplate(name, oldStatus, newStatus);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Status change email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send status change email to: {}", to, e);
        }
    }

    @Async
    public void sendRoleChangeEmail(String to, String name, String oldRole, String newRole) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(appName + " - Account Role Updated");

            String htmlContent = buildRoleChangeTemplate(name, oldRole, newRole);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Role change email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send role change email to: {}", to, e);
        }
    }

    @Async
    public void sendAccountUnlockedEmail(String to, String name) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(appName + " - Account Unlocked");

            String htmlContent = buildAccountUnlockedTemplate(name);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Account unlocked email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send account unlocked email to: {}", to, e);
        }
    }

    @Async
    public void sendAccountDeletedEmail(String to, String name) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(appName + " - Account Deactivated");

            String htmlContent = buildAccountDeletedTemplate(name);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Account deleted email sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send account deleted email to: {}", to, e);
        }
    }

    private String buildOtpEmailTemplate(String name, String otp, String purpose) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: linear-gradient(to right, #16a34a, #f59e0b, #ef4444); padding: 20px; text-align: center; }
                        .header h1 { color: white; margin: 0; }
                        .content { padding: 30px; background: #f9fafb; }
                        .otp-box { background: #16a34a; color: white; font-size: 32px; font-weight: bold; text-align: center; padding: 20px; border-radius: 8px; letter-spacing: 8px; margin: 20px 0; }
                        .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>BEFA Press</h1>
                        </div>
                        <div class="content">
                            <h2>Hello, %s!</h2>
                            <p>Your verification code for <strong>%s</strong> is:</p>
                            <div class="otp-box">%s</div>
                            <p>This code will expire in <strong>10 minutes</strong>.</p>
                            <p>If you didn't request this code, please ignore this email.</p>
                        </div>
                        <div class="footer">
                            <p>&copy; 2024 BEFA Press - Breaking Ethiopian Facts & Articles</p>
                        </div>
                    </div>
                </body>
                </html>
                """
                .formatted(name, purpose, otp);
    }

    private String buildWelcomeEmailTemplate(String name) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: linear-gradient(to right, #16a34a, #f59e0b, #ef4444); padding: 20px; text-align: center; }
                        .header h1 { color: white; margin: 0; }
                        .content { padding: 30px; background: #f9fafb; }
                        .btn { display: inline-block; background: #16a34a; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; margin-top: 20px; }
                        .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Welcome to BEFA Press!</h1>
                        </div>
                        <div class="content">
                            <h2>Hello, %s!</h2>
                            <p>Welcome to BEFA Press - Breaking Ethiopian Facts & Articles.</p>
                            <p>Your account has been verified. You can now:</p>
                            <ul>
                                <li>Write and publish opinion articles</li>
                                <li>Engage in intellectual discussions</li>
                                <li>Share your expertise with our community</li>
                            </ul>
                            <p>Start sharing your insights today!</p>
                        </div>
                        <div class="footer">
                            <p>&copy; 2024 BEFA Press - Breaking Ethiopian Facts & Articles</p>
                        </div>
                    </div>
                </body>
                </html>
                """
                .formatted(name);
    }

    private String buildOpinionStatusEmailTemplate(String name, String title, boolean approved) {
        String status = approved ? "approved and published" : "requires some revisions";
        String color = approved ? "#16a34a" : "#f59e0b";

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: linear-gradient(to right, #16a34a, #f59e0b, #ef4444); padding: 20px; text-align: center; }
                        .header h1 { color: white; margin: 0; }
                        .content { padding: 30px; background: #f9fafb; }
                        .status { background: %s; color: white; padding: 10px 20px; border-radius: 6px; display: inline-block; }
                        .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>BEFA Press</h1>
                        </div>
                        <div class="content">
                            <h2>Hello, %s!</h2>
                            <p>Your opinion article "<strong>%s</strong>" has been <span class="status">%s</span>.</p>
                            <p>Thank you for contributing to BEFA Press!</p>
                        </div>
                        <div class="footer">
                            <p>&copy; 2024 BEFA Press - Breaking Ethiopian Facts & Articles</p>
                        </div>
                    </div>
                </body>
                </html>
                """
                .formatted(color, name, title, status);
    }

    private String buildWelcomeWithPasswordTemplate(String name, String tempPassword) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: linear-gradient(to right, #16a34a, #f59e0b, #ef4444); padding: 20px; text-align: center; }
                        .header h1 { color: white; margin: 0; }
                        .content { padding: 30px; background: #f9fafb; }
                        .password-box { background: #16a34a; color: white; font-size: 24px; font-weight: bold; text-align: center; padding: 20px; border-radius: 8px; margin: 20px 0; }
                        .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                        .warning { background: #fef3c7; border-left: 4px solid #f59e0b; padding: 12px; margin: 16px 0; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>BEFA Press</h1>
                        </div>
                        <div class="content">
                            <h2>Welcome, %s!</h2>
                            <p>An administrator has created an account for you at BEFA Press.</p>
                            <p>Your temporary password is:</p>
                            <div class="password-box">%s</div>
                            <div class="warning">
                                <strong>Important:</strong> Please change your password after your first login.
                            </div>
                            <p>You can now log in using your email and this password.</p>
                        </div>
                        <div class="footer">
                            <p>&copy; 2024 BEFA Press - Breaking Ethiopian Facts & Articles</p>
                        </div>
                    </div>
                </body>
                </html>
                """
                .formatted(name, tempPassword);
    }

    private String buildPasswordResetTemplate(String name, String newPassword) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: linear-gradient(to right, #16a34a, #f59e0b, #ef4444); padding: 20px; text-align: center; }
                        .header h1 { color: white; margin: 0; }
                        .content { padding: 30px; background: #f9fafb; }
                        .password-box { background: #ef4444; color: white; font-size: 24px; font-weight: bold; text-align: center; padding: 20px; border-radius: 8px; margin: 20px 0; }
                        .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                        .warning { background: #fef3c7; border-left: 4px solid #f59e0b; padding: 12px; margin: 16px 0; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>BEFA Press</h1>
                        </div>
                        <div class="content">
                            <h2>Hello, %s!</h2>
                            <p>An administrator has reset your password.</p>
                            <p>Your new password is:</p>
                            <div class="password-box">%s</div>
                            <div class="warning">
                                <strong>Important:</strong> Please change your password immediately after logging in.
                            </div>
                            <p>If you did not request this reset, please contact support.</p>
                        </div>
                        <div class="footer">
                            <p>&copy; 2024 BEFA Press - Breaking Ethiopian Facts & Articles</p>
                        </div>
                    </div>
                </body>
                </html>
                """
                .formatted(name, newPassword);
    }

    private String buildStatusChangeTemplate(String name, String oldStatus, String newStatus) {
        String color = "ACTIVE".equals(newStatus) ? "#16a34a" : "#ca8a04";
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: linear-gradient(to right, #16a34a, #f59e0b, #ef4444); padding: 20px; text-align: center; }
                        .header h1 { color: white; margin: 0; }
                        .content { padding: 30px; background: #f9fafb; }
                        .status-box { background: %s; color: white; padding: 10px 20px; border-radius: 6px; display: inline-block; font-weight: bold; }
                        .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>BEFA Press</h1>
                        </div>
                        <div class="content">
                            <h2>Hello, %s!</h2>
                            <p>Your account status has been updated by an administrator.</p>
                            <p>Previous Status: <strong>%s</strong></p>
                            <p>New Status: <span class="status-box">%s</span></p>
                            <p>If you have any questions, please contact support.</p>
                        </div>
                        <div class="footer">
                            <p>&copy; 2024 BEFA Press - Breaking Ethiopian Facts & Articles</p>
                        </div>
                    </div>
                </body>
                </html>
                """
                .formatted(color, name, oldStatus, newStatus);
    }

    private String buildRoleChangeTemplate(String name, String oldRole, String newRole) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: linear-gradient(to right, #16a34a, #f59e0b, #ef4444); padding: 20px; text-align: center; }
                        .header h1 { color: white; margin: 0; }
                        .content { padding: 30px; background: #f9fafb; }
                        .role-box { background: #3b82f6; color: white; padding: 10px 20px; border-radius: 6px; display: inline-block; font-weight: bold; }
                        .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>BEFA Press</h1>
                        </div>
                        <div class="content">
                            <h2>Hello, %s!</h2>
                            <p>Your account role has been updated by an administrator.</p>
                            <p>Previous Role: <strong>%s</strong></p>
                            <p>New Role: <span class="role-box">%s</span></p>
                            <p>You may now have different permissions on the platform.</p>
                        </div>
                        <div class="footer">
                            <p>&copy; 2024 BEFA Press - Breaking Ethiopian Facts & Articles</p>
                        </div>
                    </div>
                </body>
                </html>
                """
                .formatted(name, oldRole.replace("ROLE_", ""), newRole.replace("ROLE_", ""));
    }

    private String buildAccountUnlockedTemplate(String name) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: linear-gradient(to right, #16a34a, #f59e0b, #ef4444); padding: 20px; text-align: center; }
                        .header h1 { color: white; margin: 0; }
                        .content { padding: 30px; background: #f9fafb; }
                        .success-box { background: #16a34a; color: white; padding: 15px; border-radius: 6px; text-align: center; font-weight: bold; }
                        .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>BEFA Press</h1>
                        </div>
                        <div class="content">
                            <h2>Hello, %s!</h2>
                            <div class="success-box">Your account has been successfully unlocked!</div>
                            <p>You can now log in to your account. Please ensure you remember your credentials.</p>
                            <p>If you have trouble logging in, please contact support.</p>
                        </div>
                        <div class="footer">
                            <p>&copy; 2024 BEFA Press - Breaking Ethiopian Facts & Articles</p>
                        </div>
                    </div>
                </body>
                </html>
                """
                .formatted(name);
    }

    private String buildAccountDeletedTemplate(String name) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: linear-gradient(to right, #16a34a, #f59e0b, #ef4444); padding: 20px; text-align: center; }
                        .header h1 { color: white; margin: 0; }
                        .content { padding: 30px; background: #f9fafb; }
                        .alert-box { background: #ef4444; color: white; padding: 15px; border-radius: 6px; text-align: center; font-weight: bold; }
                        .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>BEFA Press</h1>
                        </div>
                        <div class="content">
                            <h2>Hello, %s!</h2>
                            <div class="alert-box">Your account has been deactivated.</div>
                            <p>An administrator has deactivated or deleted your account from our system.</p>
                            <p>If you believe this is a mistake, please contact support immediately.</p>
                        </div>
                        <div class="footer">
                            <p>&copy; 2024 BEFA Press - Breaking Ethiopian Facts & Articles</p>
                        </div>
                    </div>
                </body>
                </html>
                """
                .formatted(name);
    }
}
