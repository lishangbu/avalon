package io.github.lishangbu.avalon.identity.application.model;

import java.security.Principal;
import java.util.Set;

/**
 * 认证成功后的内部身份上下文，不是面向浏览器的账号查询响应。 accountId 用于所有角色归属验证，sessionId 用于当前设备注销，roles 构建 Spring
 * Security 权限身份。 字段只能由会话核验生成，禁止根据请求体构造并用于授权。
 */
public record SessionViewer(long accountId, long sessionId, Set<String> roles) implements Principal {
	@Override
	public String getName() {
		return Long.toString(accountId);
	}
}
