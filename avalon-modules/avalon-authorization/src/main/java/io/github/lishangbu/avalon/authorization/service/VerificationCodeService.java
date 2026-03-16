package io.github.lishangbu.avalon.authorization.service;

/// 验证码服务
///
/// 仅提供验证码生成与校验的基础能力
///
/// @author lishangbu
/// @since 2026/3/13
public interface VerificationCodeService {

    /// 生成验证码并完成发送逻辑
    ///
    /// @param target 接收目标（手机号/邮箱等）
    /// @param type   验证码类型（可复用 AuthorizationGrantType 的 value 值）
    /// @return 生成的验证码
    String generateCode(String target, String type);

    /// 校验验证码（成功后会使验证码失效）
    ///
    /// @param target 接收目标（手机号/邮箱等）
    /// @param code   验证码
    /// @param type   验证码类型（可复用 AuthorizationGrantType 的 value 值）
    void verifyCode(String target, String code, String type);
}
