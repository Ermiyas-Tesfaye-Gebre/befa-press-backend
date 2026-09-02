package com.befapress.service;

import com.befapress.dto.request.LoginRequest;
import com.befapress.dto.request.RegisterRequest;
import com.befapress.dto.request.VerifyOtpRequest;
import com.befapress.dto.response.AuthResponse;
import com.befapress.dto.response.MessageResponse;
import com.befapress.dto.response.UserResponse;
import com.befapress.entity.OtpVerification;
import com.befapress.entity.Role;
import com.befapress.entity.User;
import com.befapress.exception.BadRequestException;
import com.befapress.exception.ResourceNotFoundException;
import com.befapress.repository.OtpRepository;
import com.befapress.repository.RoleRepository;
import com.befapress.repository.UserRepository;
import com.befapress.repository.LoginHistoryRepository;
import com.befapress.entity.LoginHistory;
import com.befapress.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import com.befapress.service.SmsService;

@Service
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class AuthService {

        private final UserRepository userRepository;
        private final RoleRepository roleRepository;
        private final OtpRepository otpRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtTokenProvider jwtTokenProvider;
        private final AuthenticationManager authenticationManager;
        private final UserDetailsService userDetailsService;
        private final EmailService emailService;
        private final SmsService smsService;
        private final LoginHistoryRepository loginHistoryRepository;
        private final SettingsService settingsService;

        private static final String INTELLECTUAL_ROLE = "ROLE_INTELLECTUAL";
        private static final String OTP_PURPOSE_REGISTRATION = "REGISTRATION";
        private static final String OTP_PURPOSE_LOGIN = "LOGIN";
        private static final String OTP_PURPOSE_PASSWORD_RESET = "PASSWORD_RESET";

        @Transactional
        public MessageResponse register(RegisterRequest request) {
                // Check if email already exists
                if (userRepository.existsByEmail(request.getEmail())) {
                        throw new BadRequestException("Email already registered");
                }

                // CHECK SETTINGS
                if (!settingsService.getSettings().getRegistrationEnabled()) {
                        throw new BadRequestException("User registration is currently disabled.");
                }

                Role userRole;

                if (request.getRole() != null && "INTELLECTUAL".equalsIgnoreCase(request.getRole())) {
                        userRole = roleRepository.findByName(INTELLECTUAL_ROLE)
                                        .orElseThrow(() -> new ResourceNotFoundException("Role", "name",
                                                        INTELLECTUAL_ROLE));

                        // INTELLECTUAL VALIDATIONS
                        // 1. Phone Number Required
                        if (request.getPhoneNumber() == null || request.getPhoneNumber().isEmpty()) {
                                throw new BadRequestException("Phone number is required for Intellectuals");
                        }
                        // 2. Face Capture Required
                        if (request.getFaceImage() == null || request.getFaceDescriptor() == null) {
                                throw new BadRequestException("Face verification is required for Intellectuals");
                        }

                        // 3. Face Deduplication (Euclidean Distance < 0.6 check)
                        validateUniqueFace(request.getFaceDescriptor());
                } else {
                        // Default to ROLE_USER
                        userRole = roleRepository.findByName("ROLE_USER")
                                        .orElseThrow(() -> new ResourceNotFoundException("Role", "name", "ROLE_USER"));
                }

                // Create user
                User.UserBuilder userBuilder = User.builder()
                                .fullName(request.getFullName())
                                .email(request.getEmail())
                                .passwordHash(passwordEncoder.encode(request.getPassword()))
                                .bio(request.getBio())
                                .affiliation(request.getAffiliation())
                                .expertiseField(request.getExpertiseField())
                                .role(userRole)
                                .status("PENDING")
                                .isEmailVerified(false)
                                .isVerified(false)
                                .phoneNumber(request.getPhoneNumber())
                                .faceImage(request.getFaceImage())
                                .faceDescriptor(request.getFaceDescriptor())
                                .otpMethod(request.getOtpMethod());

                User user = userBuilder.build();
                user = userRepository.save(user);

                // Generate and send OTP
                String otpCode = generateOtp();
                saveOtp(user, otpCode, OTP_PURPOSE_REGISTRATION);

                if ("SMS".equalsIgnoreCase(request.getOtpMethod()) && request.getPhoneNumber() != null) {
                        smsService.sendSms(request.getPhoneNumber(), "Your verification code is: " + otpCode);
                        return MessageResponse.success(
                                        "Registration successful. Please check your phone for verification code.");
                } else {
                        emailService.sendOtpEmail(user.getEmail(), user.getFullName(), otpCode, "Email Verification");
                        return MessageResponse.success(
                                        "Registration successful. Please check your email for verification code.");
                }
        }

        private void validateUniqueFace(String newDescriptorJson) {
                try {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        float[] newDescriptor = mapper.readValue(newDescriptorJson, float[].class);

                        java.util.List<User> intellectuals = userRepository.findAllWithFaceDescriptor();

                        for (User existingUser : intellectuals) {
                                float[] existingDescriptor = mapper.readValue(existingUser.getFaceDescriptor(),
                                                float[].class);
                                if (calculateEuclideanDistance(newDescriptor, existingDescriptor) < 0.5091) {
                                        throw new BadRequestException(
                                                        "Duplicate identity detected. You are already registered.");
                                }
                        }
                } catch (Exception e) {
                        if (e instanceof BadRequestException)
                                throw (BadRequestException) e;
                        log.error("Face validation error", e);
                        // Proceed if parsing fails to avoid blocking valid users on technical error?
                        // Or block? Security first: Block.
                        throw new BadRequestException("Face validation failed. Please try capturing again.");
                }
        }

        private double calculateEuclideanDistance(float[] f1, float[] f2) {
                double sum = 0.0;
                for (int i = 0; i < f1.length; i++) {
                        sum += Math.pow(f1[i] - f2[i], 2);
                }
                return Math.sqrt(sum);
        }

        @Transactional
        public AuthResponse login(LoginRequest request) {
                try {
                        // Authenticate user
                        authenticationManager.authenticate(
                                        new UsernamePasswordAuthenticationToken(request.getEmail(),
                                                        request.getPassword()));

                        User user = userRepository.findByEmail(request.getEmail())
                                        .orElseThrow(() -> new ResourceNotFoundException("User", "email",
                                                        request.getEmail()));

                        // Check if email is verified
                        if (!user.isEmailVerified()) {
                                // Send new OTP
                                String otpCode = generateOtp();
                                saveOtp(user, otpCode, OTP_PURPOSE_LOGIN);
                                emailService.sendOtpEmail(user.getEmail(), user.getFullName(), otpCode,
                                                "Login Verification");
                                recordLoginHistory(user.getId(), user.getEmail(), "FAILED", "Email not verified");
                                throw new BadRequestException(
                                                "Email not verified. A new verification code has been sent.");
                        }

                        // Update last login
                        user.setLastLogin(LocalDateTime.now());
                        user.setFailedLoginAttempts(0);
                        userRepository.save(user);

                        // Record successful login
                        recordLoginHistory(user.getId(), user.getEmail(), "SUCCESS", null);

                        return generateAuthResponse(user);
                } catch (org.springframework.security.core.AuthenticationException e) {
                        // Record failed login
                        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
                                user.setFailedLoginAttempts(user.getFailedLoginAttempts() != null
                                                ? user.getFailedLoginAttempts() + 1
                                                : 1);
                                if (user.getFailedLoginAttempts() >= 5) {
                                        user.setStatus("LOCKED");
                                        user.setLockedAt(LocalDateTime.now());
                                }
                                userRepository.save(user);
                                recordLoginHistory(user.getId(), user.getEmail(), "FAILED", "Invalid credentials");
                        });
                        throw e;
                }
        }

        private void recordLoginHistory(Long userId, String email, String status, String reason) {
                try {
                        LoginHistory history = LoginHistory.builder()
                                        .userId(userId)
                                        .userEmail(email)
                                        .status(status)
                                        .failureReason(reason)
                                        .build();
                        loginHistoryRepository.save(history);
                } catch (Exception e) {
                        log.error("Failed to record login history", e);
                }
        }

        @Transactional
        public AuthResponse verifyOtp(VerifyOtpRequest request) {
                User user;
                if (request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty()) {
                        user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                                        .orElseThrow(() -> new ResourceNotFoundException("User", "phone number",
                                                        request.getPhoneNumber()));
                } else {
                        user = userRepository.findByEmail(request.getEmail())
                                        .orElseThrow(() -> new ResourceNotFoundException("User", "email",
                                                        request.getEmail()));
                }

                OtpVerification otp = otpRepository.findByUserIdAndOtpCodeAndPurposeAndIsUsedFalseAndExpiresAtAfter(
                                user.getId(), request.getOtpCode(), OTP_PURPOSE_REGISTRATION, LocalDateTime.now())
                                .or(() -> otpRepository
                                                .findByUserIdAndOtpCodeAndPurposeAndIsUsedFalseAndExpiresAtAfter(
                                                                user.getId(), request.getOtpCode(),
                                                                OTP_PURPOSE_LOGIN, LocalDateTime.now()))
                                .orElseThrow(() -> new BadRequestException("Invalid or expired OTP"));

                // Mark OTP as used
                otp.setUsed(true);
                otpRepository.save(otp);

                // Mark email as verified
                user.setEmailVerified(true);
                user.setStatus("ACTIVE");
                user.setLastLogin(LocalDateTime.now());
                userRepository.save(user);

                return generateAuthResponse(user);
        }

        @Transactional
        public AuthResponse refreshToken(String refreshToken) {
                log.info("Refreshing token");
                try {
                        String email = jwtTokenProvider.extractUsername(refreshToken);
                        log.info("Email extracted: {}", email);
                        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                        if (!jwtTokenProvider.isTokenValid(refreshToken, userDetails)) {
                                log.error("Invalid token");
                                throw new BadRequestException("Invalid refresh token");
                        }

                        User user = userRepository.findByEmail(email)
                                        .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
                        log.info("User found: {}", user.getEmail());

                        String newAccessToken = jwtTokenProvider.generateToken(userDetails);

                        return AuthResponse.builder()
                                        .accessToken(newAccessToken)
                                        .refreshToken(refreshToken)
                                        .tokenType("Bearer")
                                        .expiresIn(jwtTokenProvider.getExpirationTime())
                                        .user(mapToUserResponse(user))
                                        .build();
                } catch (Exception e) {
                        log.error("Error refreshing token", e);
                        throw e;
                }
        }

        @Transactional
        public MessageResponse forgotPassword(String email) {
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

                String otpCode = generateOtp();
                saveOtp(user, otpCode, OTP_PURPOSE_PASSWORD_RESET);
                emailService.sendOtpEmail(user.getEmail(), user.getFullName(), otpCode, "Password Reset");

                return MessageResponse.success("Password reset code sent to your email.");
        }

        @Transactional
        public MessageResponse resetPassword(String email, String otpCode, String newPassword) {
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

                OtpVerification otp = otpRepository.findByUserEmailAndOtpCodeAndPurposeAndIsUsedFalseAndExpiresAtAfter(
                                email, otpCode, OTP_PURPOSE_PASSWORD_RESET, LocalDateTime.now())
                                .orElseThrow(() -> new BadRequestException("Invalid or expired OTP"));

                // Mark OTP as used
                otp.setUsed(true);
                otpRepository.save(otp);

                // Update password
                user.setPasswordHash(passwordEncoder.encode(newPassword));
                userRepository.save(user);

                return MessageResponse.success("Password reset successful. You can now login with your new password.");
        }

        public UserResponse getProfile(String email) {
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
                return mapToUserResponse(user);
        }

        private AuthResponse generateAuthResponse(User user) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
                String accessToken = jwtTokenProvider.generateToken(userDetails);
                String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

                return AuthResponse.builder()
                                .accessToken(accessToken)
                                .refreshToken(refreshToken)
                                .tokenType("Bearer")
                                .expiresIn(jwtTokenProvider.getExpirationTime())
                                .user(mapToUserResponse(user))
                                .build();
        }

        @Transactional
        public MessageResponse resendOtp(com.befapress.dto.request.ResendOtpRequest request) {
                User user;
                boolean isSms = false;

                if (request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty()) {
                        user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                                        .orElseThrow(() -> new ResourceNotFoundException("User", "phone number",
                                                        request.getPhoneNumber()));
                        isSms = true;
                } else {
                        user = userRepository.findByEmail(request.getEmail())
                                        .orElseThrow(() -> new ResourceNotFoundException("User", "email",
                                                        request.getEmail()));
                }

                String otpCode = generateOtp();
                // Determine purpose based on user status/verification status or default to
                // REGISTRATION/LOGIN?
                // For simplicity, we'll assume REGISTRATION or LOGIN based on if they are
                // already verified?
                // Actually, VerifyOtp checks both. So we can save as REGISTRATION if not
                // verified, or LOGIN if verified?
                // Let's stick to REGISTRATION for now or make it generic.
                // Better approach: Check if user is active. If active, maybe it's LOGIN
                // (two-factor). If pending, REGISTRATION.
                String purpose = user.getStatus().equals("ACTIVE") ? OTP_PURPOSE_LOGIN : OTP_PURPOSE_REGISTRATION;

                saveOtp(user, otpCode, purpose);

                if (isSms) {
                        try {
                                smsService.sendSms(user.getPhoneNumber(), "Your verification code is: " + otpCode);
                                return MessageResponse.success("Verification code resent to your phone.");
                        } catch (Exception e) {
                                log.error("SMS sending failed: {}", e.getMessage());
                                return MessageResponse.success("SMS Failed (Gateway Error). TEST CODE: " + otpCode);
                        }
                } else {
                        try {
                                emailService.sendOtpEmail(user.getEmail(), user.getFullName(), otpCode,
                                                "Verification Code");
                                return MessageResponse.success("Verification code resent to your email.");
                        } catch (Exception e) {
                                log.error("Email sending failed: {}", e.getMessage());
                                return MessageResponse.success("Email Failed. TEST CODE: " + otpCode);
                        }
                }
        }

        private String generateOtp() {
                SecureRandom random = new SecureRandom();
                int otp = 100000 + random.nextInt(900000);
                return String.valueOf(otp);
        }

        private void saveOtp(User user, String otpCode, String purpose) {
                // Delete any existing OTP for this user and purpose
                otpRepository.deleteByUserIdAndPurpose(user.getId(), purpose);

                OtpVerification otp = OtpVerification.builder()
                                .user(user)
                                .otpCode(otpCode)
                                .purpose(purpose)
                                .expiresAt(LocalDateTime.now().plusMinutes(10))
                                .isUsed(false)
                                .attempts(0)
                                .build();

                otpRepository.save(otp);
        }

        private UserResponse mapToUserResponse(User user) {
                return UserResponse.builder()
                                .id(user.getId())
                                .fullName(user.getFullName())
                                .email(user.getEmail())
                                .bio(user.getBio())
                                .profilePic(user.getProfilePic())
                                .affiliation(user.getAffiliation())
                                .expertiseField(user.getExpertiseField())
                                .isVerified(user.isVerified())
                                .isEmailVerified(user.isEmailVerified())
                                .status(user.getStatus())
                                .role(user.getRole().getName())
                                .lastLogin(user.getLastLogin())
                                .createdAt(user.getCreatedAt())
                                .build();
        }

        @Transactional
        public UserResponse updateProfile(String email, com.befapress.dto.request.UpdateProfileRequest request) {
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

                user.setFullName(request.getFullName());
                if (request.getBio() != null)
                        user.setBio(request.getBio());
                if (request.getAffiliation() != null)
                        user.setAffiliation(request.getAffiliation());
                if (request.getExpertiseField() != null)
                        user.setExpertiseField(request.getExpertiseField());
                if (request.getProfilePic() != null)
                        user.setProfilePic(request.getProfilePic());

                userRepository.save(user);

                return mapToUserResponse(user);
        }
}
