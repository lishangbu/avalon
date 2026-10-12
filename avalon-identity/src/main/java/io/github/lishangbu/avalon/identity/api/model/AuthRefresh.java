package io.github.lishangbu.avalon.identity.api.model;

/**
 * 刷新请求只接受当前刷新令牌，账号归属从摘要匹配的会话获取，不能由请求指定。
 */
public record AuthRefresh(String refreshToken) {
}
