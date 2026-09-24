package io.github.lishangbu.avalon.identity.security;

import io.github.lishangbu.avalon.identity.application.query.SessionQueryService;
import io.github.lishangbu.avalon.identity.application.model.SessionViewer;
import io.github.lishangbu.avalon.platform.BusinessException;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;

/**
 * 每次请求核验持久化会话，不缓存撤销状态。仅捕获认证错误， 业务异常由 MVC advice 处理，不能把业务失败误报为登录失效。
 */
public class BearerAuthenticationFilter extends OncePerRequestFilter {

	private final SessionQueryService sessions;

	public BearerAuthenticationFilter(SessionQueryService sessions) {
		this.sessions = sessions;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String header = request.getHeader("Authorization");
		if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
			SessionViewer viewer;
			try {
				viewer = sessions.authenticate(header.substring(7));
			}
			catch (BusinessException error) {
				viewer = null;
			}
			if (viewer == null) {
				response.setStatus(401);
				response.setHeader("WWW-Authenticate", "Bearer");
				response.setContentType("application/json;charset=UTF-8");
				response.getWriter().write("{\"message\":\"登录已失效，请重新登录\"}");
				return;
			}
			var identity = new UsernamePasswordAuthenticationToken(viewer, null,
					viewer.roles().stream().map(SimpleGrantedAuthority::new).toList());
			SecurityContextHolder.getContext().setAuthentication(identity);
		}
		chain.doFilter(request, response);
	}

}
