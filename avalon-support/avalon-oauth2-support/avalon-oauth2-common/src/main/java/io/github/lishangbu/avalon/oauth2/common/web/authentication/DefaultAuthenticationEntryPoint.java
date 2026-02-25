package io.github.lishangbu.avalon.oauth2.common.web.authentication;

import io.github.lishangbu.avalon.oauth2.common.result.SecurityErrorResultCode;
import io.github.lishangbu.avalon.web.util.JsonResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.ExceptionTranslationFilter;

/// 统一的认证入口点
///
/// 在未认证或认证失败时返回统一的 JSON 错误响应（HTTP 401 + 业务错误码）
///
/// 被 {@link ExceptionTranslationFilter} 在需要发起认证流程时调用
///
/// @author lishangbu
/// @since 2025/8/22
@Slf4j
public class DefaultAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) {
        log.error(
                "AuthenticationEntryPoint invoked for request [{}], reason=[{}]",
                request.getRequestURI(),
                authException.getMessage());

        JsonResponseWriter.writeFailedResponse(
                response,
                HttpStatus.UNAUTHORIZED,
                SecurityErrorResultCode.UNAUTHORIZED,
                authException.getMessage());
    }
}
