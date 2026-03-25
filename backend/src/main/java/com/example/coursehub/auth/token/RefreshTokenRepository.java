package com.example.coursehub.auth.token;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);
    Optional<RefreshToken> findByToken(String token);
    int deleteByExpiresAtBefore(LocalDateTime dateTime);
    List<RefreshToken> findByUserId(Long userId);
    Optional<RefreshToken> findBySessionId(String sessionId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.user.id = :userId")
    int revokeAllByUserId(@Param("userId") Long userId);
}
