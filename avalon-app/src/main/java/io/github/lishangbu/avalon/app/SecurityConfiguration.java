package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.identity.application.query.SessionQueryService;
import io.github.lishangbu.avalon.identity.security.BearerAuthenticationFilter;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

/** 浏览器采用显式 Bearer 头，不创建 Servlet 登录会话；静态资源公开，游戏 API 默认认证。 */
@Configuration
public class SecurityConfiguration {

	@Bean
	SecurityFilterChain security(HttpSecurity http, SessionQueryService sessions) throws Exception {
		return http.csrf(csrf -> csrf.disable())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.requestCache(cache -> cache.disable())
			.formLogin(form -> form.disable())
			.httpBasic(basic -> basic.disable())
			.logout(logout -> logout.disable())
			.authorizeHttpRequests(auth -> auth.dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD)
				.permitAll()
				.requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/refresh", "/actuator/health",
						"/actuator/health/**")
				.permitAll()
				.requestMatchers("/api/**")
				.authenticated()
				.anyRequest()
				.permitAll())
			.exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, failure) -> {
				response.setStatus(401);
				response.setHeader("WWW-Authenticate", "Bearer");
				response.setContentType("application/json;charset=UTF-8");
				response.getWriter().write("{\"message\":\"请先登录\"}");
			}))
			.headers(headers -> headers
				.referrerPolicy(policy -> policy.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
				.contentSecurityPolicy(policy -> policy.policyDirectives(
						"default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self' data:; connect-src 'self'; object-src 'none'; base-uri 'self'; frame-ancestors 'none'")))
			.addFilterBefore(new BearerAuthenticationFilter(sessions), UsernamePasswordAuthenticationFilter.class)
			.build();
	}

}
