package io.github.lishangbu.avalon.auth.model;

/**
 * 令牌信息
 *
 * @author lishangbu
 * @since 2025/4/9
 */
public record TokenInfo(String accessToken, String refreshToken) {}
