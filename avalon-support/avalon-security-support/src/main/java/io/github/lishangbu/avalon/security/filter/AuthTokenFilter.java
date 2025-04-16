package io.github.lishangbu.avalon.security.filter;

import io.github.lishangbu.avalon.security.constant.JwtClaimConstants;
import io.github.lishangbu.avalon.security.core.UserPrincipal;
import io.github.lishangbu.avalon.security.exception.JsonWebTokenNotFoundException;
import io.github.lishangbu.avalon.security.util.JwtUtils;
import io.github.lishangbu.avalon.security.util.SecurityUrlIgnoreCache;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

public class AuthTokenFilter extends OncePerRequestFilter {
  private static final Logger log = LoggerFactory.getLogger(AuthTokenFilter.class);
  @Autowired private JwtUtils jwtUtils;

  @Autowired private SecurityUrlIgnoreCache securityUrlIgnoreCache;

  @Autowired private HandlerExceptionResolver handlerExceptionResolver;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String jwt = parseJwt(request);

    if (ObjectUtils.isEmpty(jwt)) {
      if (securityUrlIgnoreCache.shouldIgnore(request.getRequestURI())) {
        log.debug("放行URI:[{}]", request.getRequestURI());
        filterChain.doFilter(request, response);
        return;
      }
      log.warn("访问路径[{}]未携带token", request.getRequestURI());
      handlerExceptionResolver.resolveException(
          request, response, null, new JsonWebTokenNotFoundException());
      return;
    } else {
      Claims payload = null;
      try {
        payload = jwtUtils.verifyJsonWebTokenByAuthentication(jwt);
      } catch (ExpiredJwtException e) {
        log.error("JWT令牌过期:[{}]", e.getMessage());
        handlerExceptionResolver.resolveException(
            request,
            response,
            null,
            new ExpiredJwtException(e.getHeader(), e.getClaims(), e.getMessage(), e));
      }
      if (payload != null) {
        String authorities = payload.get(JwtClaimConstants.AUTHORITIES, String.class);
        UserPrincipal userDetails =
            new UserPrincipal(
                payload.get(JwtClaimConstants.USER_ID, Long.class),
                payload.getSubject(),
                null,
                StringUtils.hasText(authorities)
                    ? AuthorityUtils.createAuthorityList(authorities.split(","))
                    : AuthorityUtils.NO_AUTHORITIES);

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
      }
    }

    filterChain.doFilter(request, response);
  }

  private String parseJwt(HttpServletRequest request) {
    String headerAuth = request.getHeader(HttpHeaders.AUTHORIZATION);

    if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
      return headerAuth.substring(7);
    }

    return null;
  }
}
