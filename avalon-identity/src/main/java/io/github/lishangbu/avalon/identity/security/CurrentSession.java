package io.github.lishangbu.avalon.identity.security;

import io.github.lishangbu.avalon.identity.application.model.SessionViewer;
import io.github.lishangbu.avalon.platform.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.security.core.context.SecurityContextHolder;

/** 仅身份模块读取设备会话，其他模块只依赖已认证账号编号。 */
@Component
public class CurrentSession {

	public SessionViewer viewer() {
		var authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof SessionViewer viewer))
			throw new BusinessException(401, "请先登录");
		return viewer;
	}

}
