package io.github.lishangbu.avalon.identity.application.model;

/**
 * 注册、登录或刷新成功时唯一返回明文令牌的用例结果。 accessToken 用于短期请求认证，refreshToken 用于下一次轮换，expiresIn
 * 为访问令牌寿命秒数。 数据库仅保存摘要；严禁记录本对象的 toString、完整 JSON 或日志参数。
 */
public record SessionTokens(String accessToken, String refreshToken, long expiresIn) {
}
