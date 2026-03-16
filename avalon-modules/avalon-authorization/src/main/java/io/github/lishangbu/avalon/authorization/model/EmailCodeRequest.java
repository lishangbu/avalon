package io.github.lishangbu.avalon.authorization.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;

/// 邮箱验证码请求
///
/// @param email 邮箱
/// @author lishangbu
/// @since 2026/3/13
public record EmailCodeRequest(
        @NotEmpty(message = "邮箱不能为空") @Email(message = "邮箱不合法") String email) {}
