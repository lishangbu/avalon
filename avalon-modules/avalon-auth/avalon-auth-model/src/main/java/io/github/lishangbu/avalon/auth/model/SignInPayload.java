package io.github.lishangbu.avalon.auth.model;

import jakarta.validation.constraints.NotEmpty;

/**
 * 登陆对象
 *
 * @param username 用户名
 * @param password 密码
 * @author lishangbu
 * @since 2025/4/9
 */
public record SignInPayload(
    @NotEmpty(message = "请输入用户名") String username, @NotEmpty(message = "请输入密码") String password) {}
