package io.github.lishangbu.avalon.platform;

import org.springframework.stereotype.Component;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;

/** 请求身份来自 SecurityContext；业务层不读取客户端传入的账号编号。 */
@Component
public class CurrentAccount {

	public long id() {
		var authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()
				|| authentication instanceof AnonymousAuthenticationToken)
			throw new BusinessException(401, "请先登录");
		return Long.parseLong(authentication.getName());
	}

}
