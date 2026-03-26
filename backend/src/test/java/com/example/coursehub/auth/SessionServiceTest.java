package com.example.coursehub.auth;

import com.example.coursehub.auth.dto.SessionResponse;
import com.example.coursehub.common.exception.ErrorCode;
import com.example.coursehub.common.exception.UserError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private RedisTemplate<String, SessionData> sessionRedisTemplate;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, SessionData> sessionValueOperations;

    @Mock
    private SetOperations<String, String> stringSetOperations;

    private SessionService sessionService;

    @BeforeEach
    void setUp() {
        sessionService = new SessionService(sessionRedisTemplate, stringRedisTemplate);
        lenient().when(sessionRedisTemplate.opsForValue()).thenReturn(sessionValueOperations);
        lenient().when(stringRedisTemplate.opsForSet()).thenReturn(stringSetOperations);
    }

    @Test
    void createSession_shouldStoreSessionAndTrackUserSession() {
        Long userId = 1L;

        String sessionId = sessionService.createSession(userId, "127.0.0.1", "Mozilla");

        assertThat(sessionId).isNotBlank();
        verify(sessionValueOperations).set(
            startsWith("session:"),
            any(SessionData.class),
            eq(7L),
            eq(TimeUnit.DAYS)
        );
        verify(stringSetOperations).add("user_sessions:" + userId, sessionId);
        verify(stringRedisTemplate).expire("user_sessions:" + userId, 7L, TimeUnit.DAYS);
    }

    @Test
    void getSession_shouldReadFromPrefixedKey() {
        String sessionId = "sid-123";
        SessionData data = new SessionData(1L, "127.0.0.1", "Mozilla", LocalDateTime.now(), LocalDateTime.now());
        when(sessionValueOperations.get("session:" + sessionId)).thenReturn(data);

        SessionData result = sessionService.getSession(sessionId);

        assertThat(result).isEqualTo(data);
    }

    @Test
    void getSessions_shouldReturnEmpty_whenNoSessionsFound() {
        when(stringSetOperations.members("user_sessions:1")).thenReturn(null);

        assertThat(sessionService.getSessions(1L)).isEmpty();
    }

    @Test
    void getSessions_shouldFilterOutMissingSessionData() {
        Long userId = 1L;
        String sessionId1 = "sid-1";
        String sessionId2 = "sid-2";
        SessionData data = new SessionData(userId, "127.0.0.1", "Mozilla", LocalDateTime.now(), LocalDateTime.now());

        when(stringSetOperations.members("user_sessions:" + userId)).thenReturn(Set.of(sessionId1, sessionId2));
        when(sessionValueOperations.get("session:" + sessionId1)).thenReturn(data);
        when(sessionValueOperations.get("session:" + sessionId2)).thenReturn(null);

        var sessions = sessionService.getSessions(userId);

        assertThat(sessions).hasSize(1);
        SessionResponse session = sessions.get(0);
        assertThat(session.sessionId()).isEqualTo(sessionId1);
        assertThat(session.ip()).isEqualTo("127.0.0.1");
        assertThat(session.userAgent()).isEqualTo("Mozilla");
    }

    @Test
    void updateLastUsedAt_shouldDoNothing_whenSessionDoesNotExist() {
        when(sessionValueOperations.get("session:sid-1")).thenReturn(null);

        sessionService.updateLastUsedAt("sid-1", 1L);

        verify(sessionValueOperations, never()).set(anyString(), any(), anyLong(), any(TimeUnit.class));
        verify(stringRedisTemplate, never()).expire(anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void updateLastUsedAt_shouldUpdateSessionAndResetUserSetTtl() {
        Long userId = 1L;
        String sessionId = "sid-1";
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        SessionData existing = new SessionData(userId, "127.0.0.1", "Mozilla", createdAt, createdAt);
        when(sessionValueOperations.get("session:" + sessionId)).thenReturn(existing);

        sessionService.updateLastUsedAt(sessionId, userId);

        ArgumentCaptor<SessionData> dataCaptor = ArgumentCaptor.forClass(SessionData.class);
        verify(sessionValueOperations).set(eq("session:" + sessionId), dataCaptor.capture(), eq(7L), eq(TimeUnit.DAYS));
        SessionData updated = dataCaptor.getValue();
        assertThat(updated.userId()).isEqualTo(userId);
        assertThat(updated.createdAt()).isEqualTo(createdAt);
        assertThat(updated.lastUsedAt()).isAfterOrEqualTo(createdAt);
        verify(stringRedisTemplate).expire("user_sessions:" + userId, 7L, TimeUnit.DAYS);
    }

    @Test
    void deleteSession_shouldDeleteAndRemoveSessionId_whenSessionBelongsToUser() {
        Long userId = 1L;
        String sessionId = "sid-1";
        when(stringSetOperations.isMember("user_sessions:" + userId, sessionId)).thenReturn(true);

        sessionService.deleteSession(userId, sessionId);

        verify(sessionRedisTemplate).delete("session:" + sessionId);
        verify(stringSetOperations).remove("user_sessions:" + userId, sessionId);
    }

    @Test
    void deleteSession_shouldThrowUserError_whenSessionDoesNotBelongToUser() {
        Long userId = 1L;
        String sessionId = "sid-1";
        when(stringSetOperations.isMember("user_sessions:" + userId, sessionId)).thenReturn(false);

        assertThatThrownBy(() -> sessionService.deleteSession(userId, sessionId))
            .isInstanceOf(UserError.class)
            .satisfies(ex -> assertThat(((UserError) ex).getErrorCode()).isEqualTo(ErrorCode.SESSION_NOT_FOUND));

        verify(sessionRedisTemplate, never()).delete(anyString());
        verify(stringSetOperations, never()).remove(anyString(), any());
    }

    @Test
    void deleteAllSessions_shouldDeleteEachSessionAndUserSetKey() {
        Long userId = 1L;
        Set<String> sessionIds = Set.of("sid-1", "sid-2");
        when(stringSetOperations.members("user_sessions:" + userId)).thenReturn(sessionIds);

        Set<String> result = sessionService.deleteAllSessions(userId);

        assertThat(result).containsExactlyInAnyOrder("sid-1", "sid-2");
        verify(sessionRedisTemplate, times(1)).delete("session:sid-1");
        verify(sessionRedisTemplate, times(1)).delete("session:sid-2");
        verify(stringRedisTemplate).delete("user_sessions:" + userId);
    }

    @Test
    void deleteAllSessions_shouldStillDeleteUserSetKey_whenNoSessionIds() {
        Long userId = 1L;
        when(stringSetOperations.members("user_sessions:" + userId)).thenReturn(null);

        Set<String> result = sessionService.deleteAllSessions(userId);

        assertThat(result).isNull();
        verify(sessionRedisTemplate, never()).delete(startsWith("session:"));
        verify(stringRedisTemplate).delete("user_sessions:" + userId);
    }

    @Test
    void sessionExists_shouldReturnTrue_whenKeyExists() {
        when(sessionRedisTemplate.hasKey("session:sid-1")).thenReturn(true);

        assertThat(sessionService.sessionExists("sid-1")).isTrue();
    }

    @Test
    void sessionExists_shouldReturnFalse_whenKeyIsMissingOrNull() {
        when(sessionRedisTemplate.hasKey("session:sid-1")).thenReturn(false);
        when(sessionRedisTemplate.hasKey("session:sid-2")).thenReturn(null);

        assertThat(sessionService.sessionExists("sid-1")).isFalse();
        assertThat(sessionService.sessionExists("sid-2")).isFalse();
    }
}
