package com.befapress.service;

import com.befapress.dto.request.ChangeRoleRequest;
import com.befapress.dto.request.ChangeStatusRequest;
import com.befapress.dto.request.CreateUserRequest;
import com.befapress.dto.request.UpdateUserRequest;
import com.befapress.dto.response.MessageResponse;
import com.befapress.dto.response.UserAdminResponse;
import com.befapress.entity.AuditLog;
import com.befapress.entity.Department;
import com.befapress.entity.Role;
import com.befapress.entity.User;
import com.befapress.exception.BadRequestException;
import com.befapress.exception.ResourceNotFoundException;
import com.befapress.repository.AuditLogRepository;
import com.befapress.repository.DepartmentRepository;
import com.befapress.repository.RoleRepository;
import com.befapress.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserAdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private static final String SUPER_ADMIN_ROLE = "ROLE_SUPER_ADMIN";
    private static final List<String> VALID_STATUSES = List.of("PENDING", "ACTIVE", "SUSPENDED", "LOCKED",
            "DEACTIVATED");

    private static final String INTELLECTUAL_ROLE = "ROLE_INTELLECTUAL";

    // ================== LIST ALL USERS (for debugging) ==================
    public Page<UserAdminResponse> getAllUsers(String roleFilter, String statusFilter, Pageable pageable) {
        Page<User> users;

        if (roleFilter != null && statusFilter != null) {
            users = userRepository.findByRoleNameAndStatusAndDeletedAtIsNull(roleFilter, statusFilter, pageable);
        } else if (roleFilter != null) {
            users = userRepository.findByRoleNameAndDeletedAtIsNull(roleFilter, pageable);
        } else if (statusFilter != null) {
            users = userRepository.findByStatusAndDeletedAtIsNull(statusFilter, pageable);
        } else {
            users = userRepository.findByDeletedAtIsNull(pageable);
        }

        return users.map(this::mapToResponse);
    }

    // ================== LIST SYSTEM USERS (excludes Intellectuals and regular
    // Users) ==================
    public Page<UserAdminResponse> getSystemUsers(String statusFilter, Pageable pageable) {
        Page<User> users;
        if (statusFilter != null) {
            users = userRepository.findSystemUsersByStatus(statusFilter, pageable);
        } else {
            users = userRepository.findSystemUsers(pageable);
        }
        return users.map(this::mapToResponse);
    }

    // ================== LIST INTELLECTUALS ==================
    public Page<UserAdminResponse> getIntellectuals(String statusFilter, Pageable pageable) {
        Page<User> users;
        if (statusFilter != null) {
            users = userRepository.findByRoleNameAndStatusAndDeletedAtIsNull(INTELLECTUAL_ROLE, statusFilter, pageable);
        } else {
            users = userRepository.findByRoleNameAndDeletedAtIsNull(INTELLECTUAL_ROLE, pageable);
        }
        return users.map(this::mapToResponse);
    }

    public Page<UserAdminResponse> searchUsers(String query, Pageable pageable) {
        Page<User> users = userRepository.searchByNameOrEmail(query, pageable);
        return users.map(this::mapToResponse);
    }

    // ================== GET USER ==================
    public UserAdminResponse getUserById(Long id) {
        User user = userRepository.findByIdWithRole(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return mapToResponse(user);
    }

    // ================== CREATE USER ==================
    @Transactional
    public UserAdminResponse createUser(CreateUserRequest request, Long adminId, String adminName) {
        // Check Super Admin restriction
        if (SUPER_ADMIN_ROLE.equals(request.getRole())) {
            User admin = userRepository.findById(adminId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", adminId));
            if (!SUPER_ADMIN_ROLE.equals(admin.getRole().getName())) {
                throw new BadRequestException("Only Super Admin can create other Super Admins");
            }
        }

        // Check email uniqueness
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already in use");
        }

        Role role = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", request.getRole()));

        Department department = null;
        if (request.getDepartmentId() != null) {
            department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.getDepartmentId()));
        }

        // Generate temporary password
        String tempPassword = generateTempPassword();

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(tempPassword))
                .phoneNumber(request.getPhoneNumber())
                .role(role)
                .department(department)
                .bio(request.getBio())
                .affiliation(request.getAffiliation())
                .expertiseField(request.getExpertiseField())
                .status("ACTIVE")
                .isEmailVerified(true)
                .isVerified(false)
                .createdBy(adminId)
                .build();

        user = userRepository.save(user);

        // Send welcome email with temp password
        emailService.sendWelcomeEmailWithPassword(user.getEmail(), user.getFullName(), tempPassword);

        // Log action
        logAction(adminId, adminName, user.getId(), user.getFullName(), "CREATED",
                "User created by admin. Temporary password sent via email.");

        log.info("User {} created by admin {}", user.getEmail(), adminId);
        return mapToResponse(user);
    }

    // ================== UPDATE USER ==================
    @Transactional
    public UserAdminResponse updateUser(Long userId, UpdateUserRequest request, Long adminId, String adminName) {
        User user = userRepository.findByIdWithRole(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        String oldValues = String.format("name=%s, email=%s", user.getFullName(), user.getEmail());

        if (request.getFullName() != null)
            user.setFullName(request.getFullName());
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BadRequestException("Email already in use");
            }
            user.setEmail(request.getEmail());
        }
        if (request.getPhoneNumber() != null)
            user.setPhoneNumber(request.getPhoneNumber());
        if (request.getBio() != null)
            user.setBio(request.getBio());
        if (request.getAffiliation() != null)
            user.setAffiliation(request.getAffiliation());
        if (request.getExpertiseField() != null)
            user.setExpertiseField(request.getExpertiseField());
        if (request.getProfilePic() != null)
            user.setProfilePic(request.getProfilePic());

        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.getDepartmentId()));
            user.setDepartment(department);
        }

        user = userRepository.save(user);

        String newValues = String.format("name=%s, email=%s", user.getFullName(), user.getEmail());
        logAction(adminId, adminName, user.getId(), user.getFullName(), "UPDATED",
                String.format("Before: %s | After: %s", oldValues, newValues));

        return mapToResponse(user);
    }

    // ================== CHANGE STATUS ==================
    @Transactional
    public UserAdminResponse changeStatus(Long userId, ChangeStatusRequest request, Long adminId, String adminName) {
        if (!VALID_STATUSES.contains(request.getStatus())) {
            throw new BadRequestException("Invalid status. Must be one of: " + VALID_STATUSES);
        }

        User user = userRepository.findByIdWithRole(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        String oldStatus = user.getStatus();
        user.setStatus(request.getStatus());

        // Clear lock fields if activating
        if ("ACTIVE".equals(request.getStatus())) {
            user.setLockedAt(null);
            user.setFailedLoginAttempts(0);
        }

        // Set lock timestamp if locking
        if ("LOCKED".equals(request.getStatus())) {
            user.setLockedAt(LocalDateTime.now());
        }

        user = userRepository.save(user);

        logAction(adminId, adminName, user.getId(), user.getFullName(), "STATUS_CHANGED",
                String.format("Status changed from %s to %s. Reason: %s", oldStatus, request.getStatus(),
                        request.getReason() != null ? request.getReason() : "N/A"));

        // Send Status Change Email
        emailService.sendStatusChangeEmail(user.getEmail(), user.getFullName(), oldStatus, request.getStatus());

        return mapToResponse(user);
    }

    // ================== CHANGE ROLE ==================
    @Transactional
    public UserAdminResponse changeRole(Long userId, ChangeRoleRequest request, Long adminId, String adminName) {
        User admin = userRepository.findByIdWithRole(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", adminId));

        // Super Admin restriction
        if (SUPER_ADMIN_ROLE.equals(request.getRole()) && !SUPER_ADMIN_ROLE.equals(admin.getRole().getName())) {
            throw new BadRequestException("Only Super Admin can assign Super Admin role");
        }

        User user = userRepository.findByIdWithRole(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Cannot demote another Super Admin unless you're Super Admin
        if (SUPER_ADMIN_ROLE.equals(user.getRole().getName()) && !SUPER_ADMIN_ROLE.equals(admin.getRole().getName())) {
            throw new BadRequestException("Only Super Admin can change another Super Admin's role");
        }

        Role newRole = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", request.getRole()));

        String oldRole = user.getRole().getName();
        user.setRole(newRole);
        user = userRepository.save(user);

        logAction(adminId, adminName, user.getId(), user.getFullName(), "ROLE_CHANGED",
                String.format("Role changed from %s to %s", oldRole, request.getRole()));

        // Send Role Change Email
        emailService.sendRoleChangeEmail(user.getEmail(), user.getFullName(), oldRole, request.getRole());

        return mapToResponse(user);
    }

    // ================== UNLOCK ACCOUNT ==================
    @Transactional
    public UserAdminResponse unlockAccount(Long userId, Long adminId, String adminName) {
        User user = userRepository.findByIdWithRole(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!"LOCKED".equals(user.getStatus())) {
            throw new BadRequestException("User is not locked");
        }

        user.setStatus("ACTIVE");
        user.setLockedAt(null);
        user.setFailedLoginAttempts(0);
        user = userRepository.save(user);

        logAction(adminId, adminName, user.getId(), user.getFullName(), "UNLOCKED", "Account unlocked by admin");

        // Send Account Unlocked Email
        emailService.sendAccountUnlockedEmail(user.getEmail(), user.getFullName());

        return mapToResponse(user);
    }

    // ================== RESET PASSWORD ==================
    @Transactional
    public MessageResponse resetPassword(Long userId, Long adminId, String adminName) {
        User user = userRepository.findByIdWithRole(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Generate password reset OTP
        String otp = generateOtp();
        // Save OTP (reuse existing OTP infrastructure from AuthService)
        // For now, send a reset email with temp password

        String tempPassword = generateTempPassword();
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        userRepository.save(user);

        emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), tempPassword);

        logAction(adminId, adminName, user.getId(), user.getFullName(), "PASSWORD_RESET",
                "Password reset triggered by admin. New password sent via email.");

        return MessageResponse.success("Password reset email sent to " + user.getEmail());
    }

    // ================== DELETE USER (Soft) ==================
    @Transactional
    public MessageResponse deleteUser(Long userId, Long adminId, String adminName) {
        User user = userRepository.findByIdWithRole(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        User admin = userRepository.findByIdWithRole(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", adminId));

        // Cannot delete Super Admin unless you're Super Admin
        if (SUPER_ADMIN_ROLE.equals(user.getRole().getName()) && !SUPER_ADMIN_ROLE.equals(admin.getRole().getName())) {
            throw new BadRequestException("Only Super Admin can delete another Super Admin");
        }

        user.setDeletedAt(LocalDateTime.now());
        user.setStatus("DEACTIVATED");
        userRepository.save(user);

        logAction(adminId, adminName, user.getId(), user.getFullName(), "DELETED", "User soft-deleted by admin");

        // Send Account Deleted Email
        emailService.sendAccountDeletedEmail(user.getEmail(), user.getFullName());

        return MessageResponse.success("User deleted successfully");
    }

    // ================== HELPER METHODS ==================
    private UserAdminResponse mapToResponse(User user) {
        return UserAdminResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .bio(user.getBio())
                .profilePic(user.getProfilePic())
                .affiliation(user.getAffiliation())
                .expertiseField(user.getExpertiseField())
                .isVerified(user.isVerified())
                .isEmailVerified(user.isEmailVerified())
                .status(user.getStatus())
                .role(user.getRole() != null ? user.getRole().getName() : "ROLE_USER")
                .departmentId(user.getDepartment() != null ? user.getDepartment().getId() : null)
                .departmentName(user.getDepartment() != null ? user.getDepartment().getName() : null)
                .failedLoginAttempts(user.getFailedLoginAttempts() != null ? user.getFailedLoginAttempts() : 0)
                .lockedAt(user.getLockedAt())
                .createdBy(user.getCreatedBy())
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private void logAction(Long adminId, String adminName, Long targetId, String targetName, String action,
            String details) {
        AuditLog log = AuditLog.builder()
                .user(userRepository.findByIdWithRole(adminId).orElse(null))
                .performedByName(adminName)
                .targetUserId(targetId)
                .targetUserName(targetName)
                .action(action)
                .entityType("USER")
                .entityId(targetId)
                .details(details)
                .build();
        auditLogRepository.save(log);
    }

    private String generateTempPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789!@#$";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
}
