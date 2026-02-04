package com.befapress.repository;

import com.befapress.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findByUserIdAndOtpCodeAndPurposeAndIsUsedFalseAndExpiresAtAfter(
            Long userId, String otpCode, String purpose, LocalDateTime now);

    Optional<OtpVerification> findByUserEmailAndOtpCodeAndPurposeAndIsUsedFalseAndExpiresAtAfter(
            String email, String otpCode, String purpose, LocalDateTime now);

    void deleteByExpiresAtBefore(LocalDateTime time);

    void deleteByUserIdAndPurpose(Long userId, String purpose);
}
