package com.example.coursehub.auth;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import static org.assertj.core.api.Assertions.assertThat;

class CookieUtilsTest {

    private final CookieUtils cookieUtils = new CookieUtils();

    // createRefreshTokenCookie
    @Test
    void createRefreshTokenCookie_shouldHaveCorrectValue() {
        ResponseCookie cookie = cookieUtils.createRefreshTokenCookie("my-refresh-token");
        assertThat(cookie.getValue()).isEqualTo("my-refresh-token");
    }

    @Test
    void createRefreshTokenCookie_shouldBeHttpOnly() {
        ResponseCookie cookie = cookieUtils.createRefreshTokenCookie("token");
        assertThat(cookie.isHttpOnly()).isTrue();
    }

    @Test
    void createRefreshTokenCookie_shouldBeSecure() {
        ResponseCookie cookie = cookieUtils.createRefreshTokenCookie("token");
        assertThat(cookie.isSecure()).isTrue();
    }

    @Test
    void createRefreshTokenCookie_shouldHaveCorrectPath() {
        ResponseCookie cookie = cookieUtils.createRefreshTokenCookie("token");
        assertThat(cookie.getPath()).isEqualTo("/api/auth");
    }

    @Test
    void createRefreshTokenCookie_shouldHaveCorrectMaxAge() {
        ResponseCookie cookie = cookieUtils.createRefreshTokenCookie("token");
        assertThat(cookie.getMaxAge().toDays()).isEqualTo(7);
    }

    @Test
    void createRefreshTokenCookie_shouldHaveCorrectSameSite() {
        ResponseCookie cookie = cookieUtils.createRefreshTokenCookie("token");
        assertThat(cookie.getSameSite()).isEqualTo("Strict");
    }

    // clearRefreshTokenCookie
    @Test
    void clearRefreshTokenCookie_shouldHaveEmptyValue() {
        ResponseCookie cookie = cookieUtils.clearRefreshTokenCookie();
        assertThat(cookie.getValue()).isEmpty();
    }

    @Test
    void clearRefreshTokenCookie_shouldHaveZeroMaxAge() {
        ResponseCookie cookie = cookieUtils.clearRefreshTokenCookie();
        assertThat(cookie.getMaxAge().getSeconds()).isZero();
    }

    @Test
    void clearRefreshTokenCookie_shouldHaveSameAttributesAsCreateCookie() {
        ResponseCookie create = cookieUtils.createRefreshTokenCookie("token");
        ResponseCookie clear = cookieUtils.clearRefreshTokenCookie();

        assertThat(clear.isHttpOnly()).isEqualTo(create.isHttpOnly());
        assertThat(clear.isSecure()).isEqualTo(create.isSecure());
        assertThat(clear.getPath()).isEqualTo(create.getPath());
        assertThat(clear.getSameSite()).isEqualTo(create.getSameSite());
    }
}