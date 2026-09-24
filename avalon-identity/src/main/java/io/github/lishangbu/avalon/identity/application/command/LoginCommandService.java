package io.github.lishangbu.avalon.identity.application.command;

import io.github.lishangbu.avalon.identity.application.model.SessionTokens;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;

import org.springframework.stereotype.Component;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;

import lombok.RequiredArgsConstructor;

/**
 * 密码登录用例。Resource 只解析请求，本服务负责调用 Spring Security 校验密码并签发设备会话。 密码验证阶段不包裹应用写事务；验证通过后
 * SessionCommandService 独立提交设备会话。 错误统一为账号或密码不正确，避免向客户端区分账号不存在与密码错误。
 */
@Component
@RequiredArgsConstructor
public class LoginCommandService {

	private final AuthenticationManager providers;

	private final AccountQueryService accounts;

	private final SessionCommandService sessions;

	/**
	 * 明文密码仅用于当前认证调用，不记录日志，也不传入会话存储。
	 */
	public SessionTokens login(String username, String password, String device) {
		RegistrationService.validate(username, password);
		try {
			providers.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(username, password));
		}
		catch (AuthenticationException error) {
			throw new BusinessException(401, "账号或密码不正确");
		}
		return sessions.create(accounts.accountId(username), device);
	}

}
