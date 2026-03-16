package io.github.lishangbu.avalon.oauth2.authorizationserver.authentication;

import java.util.Collection;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

/// 邮箱验证码登录的认证令牌
///
/// 用于承载邮箱与验证码，并与 AuthenticationManager 协作完成认证
///
/// @author lishangbu
/// @since 2026/3/13
public class EmailAuthenticationToken extends UsernamePasswordAuthenticationToken {

    public EmailAuthenticationToken(String email, String emailCode) {
        super(email, emailCode);
    }

    public EmailAuthenticationToken(
            Object principal,
            Object credentials,
            Collection<? extends GrantedAuthority> authorities) {
        super(principal, credentials, authorities);
    }
}
