package com.befapress.repository;

import com.befapress.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

        Optional<User> findByEmail(String email);

        @Query("SELECT u FROM User u LEFT JOIN FETCH u.role LEFT JOIN FETCH u.department WHERE u.email = :email")
        Optional<User> findByEmailWithRole(@Param("email") String email);

        Optional<User> findByPhoneNumber(String phoneNumber);

        @Query("SELECT u FROM User u LEFT JOIN FETCH u.role LEFT JOIN FETCH u.department WHERE u.id = :id")
        Optional<User> findByIdWithRole(@Param("id") Long id);

        boolean existsByEmail(String email);

        boolean existsByPhoneNumber(String phoneNumber);

        Page<User> findByStatus(String status, Pageable pageable);

        // Get all users (with role eagerly loaded)
        @Query("SELECT u FROM User u LEFT JOIN FETCH u.role WHERE u.deletedAt IS NULL")
        Page<User> findByDeletedAtIsNull(Pageable pageable);

        // Get users by role name
        @Query("SELECT u FROM User u LEFT JOIN FETCH u.role WHERE u.role.name = :roleName AND u.deletedAt IS NULL")
        Page<User> findByRoleNameAndDeletedAtIsNull(@Param("roleName") String roleName, Pageable pageable);

        // Get users by role and status
        @Query("SELECT u FROM User u LEFT JOIN FETCH u.role WHERE u.role.name = :roleName AND u.status = :status AND u.deletedAt IS NULL")
        Page<User> findByRoleNameAndStatusAndDeletedAtIsNull(
                        @Param("roleName") String roleName,
                        @Param("status") String status,
                        Pageable pageable);

        // Get users by status (with role eagerly loaded)
        @Query("SELECT u FROM User u LEFT JOIN FETCH u.role WHERE u.status = :status AND u.deletedAt IS NULL")
        Page<User> findByStatusAndDeletedAtIsNull(@Param("status") String status, Pageable pageable);

        // Search by name or email
        @Query("SELECT u FROM User u LEFT JOIN FETCH u.role WHERE (LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))) AND u.deletedAt IS NULL")
        Page<User> searchByNameOrEmail(@Param("query") String query, Pageable pageable);

        // Get system users (non-intellectuals, non-regular users)
        @Query("SELECT u FROM User u LEFT JOIN FETCH u.role WHERE u.role.name NOT IN ('ROLE_INTELLECTUAL', 'ROLE_USER') AND u.deletedAt IS NULL")
        Page<User> findSystemUsers(Pageable pageable);

        // Get system users by status
        @Query("SELECT u FROM User u LEFT JOIN FETCH u.role WHERE u.role.name NOT IN ('ROLE_INTELLECTUAL', 'ROLE_USER') AND u.status = :status AND u.deletedAt IS NULL")
        Page<User> findSystemUsersByStatus(@Param("status") String status, Pageable pageable);

        // ========== Analytics Methods ==========

        @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :since")
        Long countByCreatedAtAfter(@Param("since") java.time.LocalDateTime since);

        @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt BETWEEN :start AND :end")
        Long countByCreatedAtBetween(@Param("start") java.time.LocalDateTime start,
                        @Param("end") java.time.LocalDateTime end);

        @Query("SELECT COUNT(u) FROM User u WHERE u.role.name = :roleName")
        Long countByRoleName(@Param("roleName") String roleName);

        @Query("SELECT u FROM User u WHERE u.faceDescriptor IS NOT NULL AND u.faceDescriptor <> ''")
        java.util.List<User> findAllWithFaceDescriptor();

        long countByDeletedAtIsNull();
}
