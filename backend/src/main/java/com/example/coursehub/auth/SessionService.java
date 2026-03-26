package com.example.coursehub.auth;

import com.example.coursehub.auth.dto.SessionResponse;
import com.example.coursehub.common.exception.ErrorCode;
import com.example.coursehub.common.exception.UserError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class SessionService {

    private final RedisTemplate<String, SessionData> sessionRedisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String SESSION_PREFIX = "session:";
    private static final String USER_SESSIONS_PREFIX = "user_sessions:";
    private static final long SESSION_TTL_DAYS = 7;

    public SessionService(RedisTemplate<String, SessionData> sessionRedisTemplate, StringRedisTemplate stringRedisTemplate) {
        this.sessionRedisTemplate = sessionRedisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public String createSession(Long userId, String ip, String userAgent) {
        String sessionId = java.util.UUID.randomUUID().toString();

        SessionData sessionData = new SessionData(
            userId,
            ip,
            userAgent,
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        // store session data
        sessionRedisTemplate.opsForValue().set(
            SESSION_PREFIX + sessionId,
            sessionData,
            SESSION_TTL_DAYS,
            TimeUnit.DAYS
        );

        // add sessionId to user's session set
        stringRedisTemplate.opsForSet().add(USER_SESSIONS_PREFIX + userId, sessionId);
        stringRedisTemplate.expire(USER_SESSIONS_PREFIX + userId, SESSION_TTL_DAYS, TimeUnit.DAYS);

        log.info("Created session {} for user {}", sessionId, userId);
        return sessionId;
    }

    public SessionData getSession(String sessionId) {
        return sessionRedisTemplate.opsForValue().get(SESSION_PREFIX + sessionId);
    }

    public List<SessionResponse> getSessions(Long userId) {
        Set<String> sessionIds = stringRedisTemplate.opsForSet().members(USER_SESSIONS_PREFIX + userId);

        if (sessionIds == null || sessionIds.isEmpty()) {
            return List.of();
        }

        return sessionIds.stream()
            .map(id -> {
                SessionData data = getSession(id);
                if (data == null) return null;
                return new SessionResponse(
                    (String) id,
                    data.ip(),
                    data.userAgent(),
                    data.createdAt(),
                    data.lastUsedAt()
                );
            })
            .filter(s -> s != null)
            .toList();
    }

    public void updateLastUsedAt(String sessionId, Long userId) {
        SessionData existing = getSession(sessionId);
        if (existing == null) return;

        SessionData updated = new SessionData(
            existing.userId(),
            existing.ip(),
            existing.userAgent(),
            existing.createdAt(),
            LocalDateTime.now()
        );

        sessionRedisTemplate.opsForValue().set(
            SESSION_PREFIX + sessionId,
            updated,
            SESSION_TTL_DAYS,
            TimeUnit.DAYS
        );

        // reset user_sessions TTL
        stringRedisTemplate.expire(USER_SESSIONS_PREFIX + userId, SESSION_TTL_DAYS, TimeUnit.DAYS);
    }

    public void deleteSession(Long userId, String sessionId) {
        // verify session belongs to user
        Boolean isMember = stringRedisTemplate.opsForSet().isMember(
            USER_SESSIONS_PREFIX + userId, sessionId
        );

        if (!Boolean.TRUE.equals(isMember)) {
            throw new UserError(ErrorCode.SESSION_NOT_FOUND);
        }

        sessionRedisTemplate.delete(SESSION_PREFIX + sessionId);
        stringRedisTemplate.opsForSet().remove(USER_SESSIONS_PREFIX + userId, sessionId);

        log.info("Deleted session {} for user {}", sessionId, userId);
    }

    public Set<String> deleteAllSessions(Long userId) {
        Set<String> sessionIds = stringRedisTemplate.opsForSet().members(USER_SESSIONS_PREFIX + userId);

        if (sessionIds != null && !sessionIds.isEmpty()) {
            sessionIds.forEach(id ->
                sessionRedisTemplate.delete(SESSION_PREFIX + id)
            );
        }

        stringRedisTemplate.delete(USER_SESSIONS_PREFIX + userId);

        log.info("Deleted all sessions for user {}", userId);
        return sessionIds;
    }

    public boolean sessionExists(String sessionId) {
        return Boolean.TRUE.equals(sessionRedisTemplate.hasKey(SESSION_PREFIX + sessionId));
    }
}
