package io.github.lishangbu.avalon.authorization.authentication;

import io.github.lishangbu.avalon.authorization.service.VerificationCodeService;
import io.github.lishangbu.avalon.oauth2.authorizationserver.authentication.EmailAuthenticationToken;
import io.github.lishangbu.avalon.oauth2.authorizationserver.exception.InvalidCaptchaException;
import io.github.lishangbu.avalon.oauth2.common.core.AuthorizationGrantTypeSupport;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/// 邮箱验证码认证提供者
///
/// 使用 Redis 中的邮箱验证码进行认证
///
/// @author lishangbu
/// @since 2026/3/13
@Component
@RequiredArgsConstructor
public class EmailCodeAuthenticationProvider implements AuthenticationProvider {

    private final VerificationCodeService verificationCodeService;
    private final UserDetailsService userDetailsService;

    @Override
    public Authentication authenticate(Authentication authentication)
            throws AuthenticationException {
        EmailAuthenticationToken emailAuthenticationToken =
                (EmailAuthenticationToken) authentication;
        String email = normalizeEmail(resolveText(emailAuthenticationToken.getPrincipal()));
        String code = resolveText(emailAuthenticationToken.getCredentials());
        if (!StringUtils.hasText(email) || !StringUtils.hasText(code)) {
            throw new InvalidCaptchaException("邮箱验证码不能为空");
        }
        verificationCodeService.verifyCode(
                email, code, AuthorizationGrantTypeSupport.EMAIL.getValue());
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        EmailAuthenticationToken authenticated =
                new EmailAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        authenticated.setDetails(authentication.getDetails());
        return authenticated;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return EmailAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private String resolveText(Object value) {
        return value == null ? null : value.toString();
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return email;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
