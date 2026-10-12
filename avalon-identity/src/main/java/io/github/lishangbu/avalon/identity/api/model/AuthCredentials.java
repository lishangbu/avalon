package io.github.lishangbu.avalon.identity.api.model;

/**
 * 登录/注册入参含敏感密码，仅用于本次用例调用，禁止直接打印整个请求对象。
 */
public record AuthCredentials(String username, String password, String device) {
}
