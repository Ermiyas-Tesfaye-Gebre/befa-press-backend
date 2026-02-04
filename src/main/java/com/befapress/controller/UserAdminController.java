package com.befapress.controller;

import com.befapress.dto.request.ChangeRoleRequest;
import com.befapress.dto.request.ChangeStatusRequest;
import com.befapress.dto.request.CreateUserRequest;
import com.befapress.dto.request.UpdateUserRequest;
import com.befapress.dto.response.MessageResponse;
import com.befapress.dto.response.UserAdminResponse;
import com.befapress.service.UserAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "User Admin", description = "Admin APIs for user management")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class UserAdminController {

    private final UserAdminService userAdminService;
    private final com.befapress.repository.UserRepository userRepository;

    // ================== LIST ALL USERS ==================
    @GetMapping
    @Operation(summary = "List all users with optional filters")
    public ResponseEntity<Page<UserAdminResponse>> getAllUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return ResponseEntity.ok(userAdminService.getAllUsers(role, status, pageable));
    }

    // ================== LIST SYSTEM USERS (Admins, Editors, etc.)
    // ==================
    @GetMapping("/system")
    @Operation(summary = "List system users (excludes Intellectuals and regular Users)")
    public ResponseEntity<Page<UserAdminResponse>> getSystemUsers(
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return ResponseEntity.ok(userAdminService.getSystemUsers(status, pageable));
    }

    // ================== LIST INTELLECTUALS ==================
    @GetMapping("/intellectuals")
    @Operation(summary = "List all intellectuals with optional status filter")
    public ResponseEntity<Page<UserAdminResponse>> getIntellectuals(
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return ResponseEntity.ok(userAdminService.getIntellectuals(status, pageable));
    }

    @GetMapping("/search")
    @Operation(summary = "Search users by name or email")
    public ResponseEntity<Page<UserAdminResponse>> searchUsers(
            @RequestParam String query,
            Pageable pageable) {
        return ResponseEntity.ok(userAdminService.searchUsers(query, pageable));
    }

    // ================== GET USER ==================
    @GetMapping("/{id}")
    @Operation(summary = "Get user details by ID")
    public ResponseEntity<UserAdminResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userAdminService.getUserById(id));
    }

    // ================== CREATE USER ==================
    @PostMapping
    @Operation(summary = "Create a new system user")
    public ResponseEntity<UserAdminResponse> createUser(
            @Valid @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long adminId = getAdminId(userDetails);
        String adminName = userDetails.getUsername();
        return ResponseEntity.ok(userAdminService.createUser(request, adminId, adminName));
    }

    // ================== UPDATE USER ==================
    @PutMapping("/{id}")
    @Operation(summary = "Update user profile")
    public ResponseEntity<UserAdminResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long adminId = getAdminId(userDetails);
        String adminName = userDetails.getUsername();
        return ResponseEntity.ok(userAdminService.updateUser(id, request, adminId, adminName));
    }

    // ================== CHANGE STATUS ==================
    @PatchMapping("/{id}/status")
    @Operation(summary = "Change user status (ACTIVE, SUSPENDED, LOCKED, DEACTIVATED)")
    public ResponseEntity<UserAdminResponse> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ChangeStatusRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long adminId = getAdminId(userDetails);
        String adminName = userDetails.getUsername();
        return ResponseEntity.ok(userAdminService.changeStatus(id, request, adminId, adminName));
    }

    // ================== CHANGE ROLE ==================
    @PatchMapping("/{id}/role")
    @Operation(summary = "Change user role")
    public ResponseEntity<UserAdminResponse> changeRole(
            @PathVariable Long id,
            @Valid @RequestBody ChangeRoleRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long adminId = getAdminId(userDetails);
        String adminName = userDetails.getUsername();
        return ResponseEntity.ok(userAdminService.changeRole(id, request, adminId, adminName));
    }

    // ================== UNLOCK ACCOUNT ==================
    @PostMapping("/{id}/unlock")
    @Operation(summary = "Unlock a locked user account")
    public ResponseEntity<UserAdminResponse> unlockAccount(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long adminId = getAdminId(userDetails);
        String adminName = userDetails.getUsername();
        return ResponseEntity.ok(userAdminService.unlockAccount(id, adminId, adminName));
    }

    // ================== RESET PASSWORD ==================
    @PostMapping("/{id}/reset-password")
    @Operation(summary = "Send password reset email to user")
    public ResponseEntity<MessageResponse> resetPassword(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long adminId = getAdminId(userDetails);
        String adminName = userDetails.getUsername();
        return ResponseEntity.ok(userAdminService.resetPassword(id, adminId, adminName));
    }

    // ================== DELETE USER ==================
    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete a user")
    public ResponseEntity<MessageResponse> deleteUser(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long adminId = getAdminId(userDetails);
        String adminName = userDetails.getUsername();
        return ResponseEntity.ok(userAdminService.deleteUser(id, adminId, adminName));
    }

    // ================== HELPER ==================
    private Long getAdminId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Admin user not found"))
                .getId();
    }
}
