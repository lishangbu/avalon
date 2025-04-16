package io.github.lishangbu.avalon.auth.model;

/**
 * 令牌信息类，包含访问令牌和刷新令牌。
 *
 * @param accessToken 访问令牌
 * @param refreshToken 刷新令牌
 * @author lishangbu
 * @since 2025/4/9
 */
public record TokenInfo(String accessToken, String refreshToken) {}
