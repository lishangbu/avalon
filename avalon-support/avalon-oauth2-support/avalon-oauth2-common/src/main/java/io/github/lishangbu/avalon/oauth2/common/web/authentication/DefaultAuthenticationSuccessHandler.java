package io.github.lishangbu.avalon.oauth2.common.web.authentication;

import io.github.lishangbu.avalon.web.util.JsonResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import tools.jackson.databind.json.JsonMapper;

/// 默认认证成功处理器
///
/// 在认证成功时返回一个统一的 JSON 成功响应
///
/// @author lishangbu
/// @since 2025/8/23
@Slf4j
public class DefaultAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JsonMapper jsonMapper;

    public DefaultAuthenticationSuccessHandler(JsonMapper jsonMapper) {
        this.jsonMapper = Objects.requireNonNull(jsonMapper, "jsonMapper");
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) {
        JsonResponseWriter.writeSuccessResponse(response, jsonMapper);
    }
}
