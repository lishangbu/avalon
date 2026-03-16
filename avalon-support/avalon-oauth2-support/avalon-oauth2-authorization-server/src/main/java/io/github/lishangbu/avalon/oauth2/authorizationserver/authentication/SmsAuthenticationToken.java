package io.github.lishangbu.avalon.oauth2.authorizationserver.authentication;

import java.util.Collection;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

/// 短信验证码登录的认证令牌
///
/// 用于承载手机号与短信验证码，并与 AuthenticationManager 协作完成认证
///
/// @author lishangbu
/// @since 2026/3/13
public class SmsAuthenticationToken extends UsernamePasswordAuthenticationToken {

    public SmsAuthenticationToken(String phoneNumber, String smsCode) {
        super(phoneNumber, smsCode);
    }

    public SmsAuthenticationToken(
            Object principal,
            Object credentials,
            Collection<? extends GrantedAuthority> authorities) {
        super(principal, credentials, authorities);
    }
}
