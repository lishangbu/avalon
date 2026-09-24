package io.github.lishangbu.avalon.identity.api;

import io.github.lishangbu.avalon.identity.application.command.LoginCommandService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.command.SessionCommandService;
import io.github.lishangbu.avalon.identity.application.model.DeviceView;
import io.github.lishangbu.avalon.identity.application.model.SessionTokens;
import io.github.lishangbu.avalon.identity.application.model.SessionViewer;
import io.github.lishangbu.avalon.identity.application.query.SessionQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.identity.security.CurrentSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * 浏览器认证入口。密码校验走 Spring Security，Token 会话由独立服务管理。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthResource {

	private final RegistrationService registrations;

	private final SessionCommandService sessions;

	private final SessionQueryService sessionQueries;

	private final LoginCommandService login;

	private final CurrentSession identity;

	@PostMapping("/register")
	public SessionTokens register(@RequestBody(required = false) Credentials body) {
		if (body == null)
			throw new BusinessException(400, "请填写账号和密码");
		return registrations.register(body.username(), body.password(), body.device());
	}

	@PostMapping("/login")
	public SessionTokens login(@RequestBody(required = false) Credentials body) {
		if (body == null)
			throw new BusinessException(400, "请填写账号和密码");
		return login.login(body.username(), body.password(), body.device());
	}

	@PostMapping("/refresh")
	public SessionTokens refresh(@RequestBody(required = false) Refresh body) {
		if (body == null)
			throw new BusinessException(401, "请重新登录");
		SessionTokens tokens = sessions.refresh(body.refreshToken());
		if (tokens == null)
			throw new BusinessException(401, "会话已失效，请重新登录");
		return tokens;
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout() {
		SessionViewer v = identity.viewer();
		sessions.revoke(v, v.sessionId());
	}

	@GetMapping("/sessions")
	public List<DeviceView> devices() {
		return sessionQueries.list(identity.viewer());
	}

	@DeleteMapping("/sessions/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void revoke(@PathVariable("id") long id) {
		sessions.revoke(identity.viewer(), id);
	}

	/**
	 * 登录/注册入参含敏感密码，仅用于本次用例调用，禁止直接打印整个请求对象。
	 */
	public record Credentials(String username, String password, String device) {
	}

	/**
	 * 刷新请求只接受当前刷新令牌，账号归属从摘要匹配的会话获取，不能由请求指定。
	 */
	public record Refresh(String refreshToken) {
	}

}
