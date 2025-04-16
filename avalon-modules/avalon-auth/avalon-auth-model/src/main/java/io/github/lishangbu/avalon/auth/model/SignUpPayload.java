package io.github.lishangbu.avalon.auth.model;

import jakarta.validation.constraints.NotEmpty;

/**
 * 注册对象
 *
 * @param username 注册用的用户名
 * @param password 注册用的密码
 * @param roleCode 角色代码
 * @since 2025/4/9
 */
public record SignUpPayload(
    @NotEmpty(message = "请输入用户名") String username,
    @NotEmpty(message = "请输入密码") String password,
    String roleCode) {}
