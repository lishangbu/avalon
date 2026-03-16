package io.github.lishangbu.avalon.authorization.authentication;

import io.github.lishangbu.avalon.authorization.service.VerificationCodeService;
import io.github.lishangbu.avalon.oauth2.authorizationserver.authentication.SmsAuthenticationToken;
import io.github.lishangbu.avalon.oauth2.authorizationserver.exception.InvalidCaptchaException;
import io.github.lishangbu.avalon.oauth2.common.core.AuthorizationGrantTypeSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/// 短信验证码认证提供者
///
/// 使用 Redis 中的短信验证码进行认证
///
/// @author lishangbu
/// @since 2026/3/13
@Component
@RequiredArgsConstructor
public class SmsCodeAuthenticationProvider implements AuthenticationProvider {

    private final VerificationCodeService verificationCodeService;
    private final UserDetailsService userDetailsService;

    @Override
    public Authentication authenticate(Authentication authentication)
            throws AuthenticationException {
        SmsAuthenticationToken smsAuthenticationToken = (SmsAuthenticationToken) authentication;
        String phone = normalizePhone(resolveText(smsAuthenticationToken.getPrincipal()));
        String code = resolveText(smsAuthenticationToken.getCredentials());
        if (!StringUtils.hasText(phone) || !StringUtils.hasText(code)) {
            throw new InvalidCaptchaException("短信验证码不能为空");
        }
        verificationCodeService.verifyCode(
                phone, code, AuthorizationGrantTypeSupport.SMS.getValue());
        UserDetails userDetails = userDetailsService.loadUserByUsername(phone);
        SmsAuthenticationToken authenticated =
                new SmsAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        authenticated.setDetails(authentication.getDetails());
        return authenticated;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return SmsAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private String resolveText(Object value) {
        return value == null ? null : value.toString();
    }

    private String normalizePhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return phone;
        }
        return phone.trim();
    }
}
