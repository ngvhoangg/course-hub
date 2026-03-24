package com.example.coursehub.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private JwtTokenProvider jwtTokenProvider;

    private Authentication authentication;

    // 256-bit base64 encoded secret for testing
    private static final String TEST_SECRET = Base64.getEncoder()
        .encodeToString("test-secret-key-must-be-256-bits!!".getBytes());

    @BeforeEach
    void setUp() {
        lenient().when(jwtProperties.getSecret()).thenReturn(TEST_SECRET);
        lenient().when(jwtProperties.getAccessTokenExpiration()).thenReturn(900000L);
        lenient().when(jwtProperties.getRefreshTokenExpiration()).thenReturn(604800000L);

        authentication = new UsernamePasswordAuthenticationToken(
            "test@example.com", null,
            List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );
    }

    // generateAccessToken
    @Test
    void generateAccessToken_shouldReturnValidToken() {
        String token = jwtTokenProvider.generateAccessToken(authentication);
        assertThat(token).isNotNull();
    }

    @Test
    void generateAccessToken_shouldContainEmail() {
        String token = jwtTokenProvider.generateAccessToken(authentication);
        assertThat(jwtTokenProvider.getEmailFromToken(token)).isEqualTo("test@example.com");
    }

    @Test
    void generateAccessToken_shouldContainRole() {
        String token = jwtTokenProvider.generateAccessToken(authentication);

        Claims claims = Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(TEST_SECRET)))
            .build()
            .parseSignedClaims(token)
            .getPayload();

        assertThat(claims.get("role")).isEqualTo("ROLE_STUDENT");
    }

    // generateRefreshToken
    @Test
    void generateRefreshToken_shouldReturnValidToken() {
        String token = jwtTokenProvider.generateRefreshToken(authentication);
        assertThat(token).isNotNull();
    }

    @Test
    void generateRefreshToken_shouldContainEmail() {
        String token = jwtTokenProvider.generateRefreshToken(authentication);
        assertThat(jwtTokenProvider.getEmailFromToken(token)).isEqualTo("test@example.com");
    }

    // validateToken
    @Test
    void validateToken_shouldReturnTrue_whenTokenIsValid() {
        String token = jwtTokenProvider.generateAccessToken(authentication);
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_shouldReturnFalse_whenTokenIsInvalid() {
        assertThat(jwtTokenProvider.validateToken("invalid.token.here")).isFalse();
    }

    @Test
    void validateToken_shouldReturnFalse_whenTokenIsExpired() {
        when(jwtProperties.getAccessTokenExpiration()).thenReturn(-1000L);
        String token = jwtTokenProvider.generateAccessToken(authentication);
        assertThat(jwtTokenProvider.validateToken(token)).isFalse();
    }

    @Test
    void validateToken_shouldReturnFalse_whenSignatureInvalid() {
        String token = jwtTokenProvider.generateAccessToken(authentication);

        when(jwtProperties.getSecret()).thenReturn(
            Base64.getEncoder().encodeToString("different-secret-key-256bits!!!".getBytes())
        );

        assertThat(jwtTokenProvider.validateToken(token)).isFalse();
    }

    // getEmailFromToken
    @Test
    void getEmailFromToken_shouldReturnCorrectEmail() {
        String token = jwtTokenProvider.generateAccessToken(authentication);
        assertThat(jwtTokenProvider.getEmailFromToken(token)).isEqualTo("test@example.com");
    }

    // getJtiFromToken
    @Test
    void getJtiFromToken_shouldReturnValidJti() {
        String token = jwtTokenProvider.generateAccessToken(authentication);
        String jti = jwtTokenProvider.getJtiFromToken(token);

        assertThat(jti).isNotNull().isNotBlank();
    }

    @Test
    void getJtiFromToken_shouldReturnUniqueJtiForEachToken() {
        String token1 = jwtTokenProvider.generateAccessToken(authentication);
        String token2 = jwtTokenProvider.generateAccessToken(authentication);

        String jti1 = jwtTokenProvider.getJtiFromToken(token1);
        String jti2 = jwtTokenProvider.getJtiFromToken(token2);

        assertThat(jti1).isNotEqualTo(jti2);
    }

    // getRemainingExpiry
    @Test
    void getRemainingExpiry_shouldReturnPositiveValue_forValidToken() {
        String token = jwtTokenProvider.generateAccessToken(authentication);
        long remainingExpiry = jwtTokenProvider.getRemainingExpiry(token);

        assertThat(remainingExpiry).isPositive();
    }

    @Test
    void getRemainingExpiry_shouldReturnLessThanTokenExpiration() {
        String token = jwtTokenProvider.generateAccessToken(authentication);
        long remainingExpiry = jwtTokenProvider.getRemainingExpiry(token);

        assertThat(remainingExpiry).isLessThan(jwtProperties.getAccessTokenExpiration());
    }
}