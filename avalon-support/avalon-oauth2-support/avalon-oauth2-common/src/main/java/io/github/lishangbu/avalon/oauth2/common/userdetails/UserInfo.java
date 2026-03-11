package io.github.lishangbu.avalon.oauth2.common.userdetails;

import java.io.Serial;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.SpringSecurityCoreVersion;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;

/// 用户信息
///
/// 封装 Spring Security User 并实现 OAuth2AuthenticatedPrincipal，支持附加参数
///
/// @author lishangbu
/// @since 2025/8/9
@Getter
@Setter
@SuppressWarnings("removal")
@EqualsAndHashCode(callSuper = true)
public class UserInfo extends User implements OAuth2AuthenticatedPrincipal {
    @Serial
    private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

    /// 附加参数：用于在获取 Token 接口返回
    @Getter private final Map<String, Object> additionalParameters = new HashMap<>();

    public UserInfo(
            String username,
            @Nullable String password,
            Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
    }

    public UserInfo(
            String username,
            @Nullable String password,
            boolean enabled,
            boolean accountNonExpired,
            boolean credentialsNonExpired,
            boolean accountNonLocked,
            Collection<? extends GrantedAuthority> authorities) {
        super(
                username,
                password,
                enabled,
                accountNonExpired,
                credentialsNonExpired,
                accountNonLocked,
                authorities);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return additionalParameters;
    }

    @Override
    @NullMarked
    public String getName() {
        return super.getUsername();
    }

    @Override
    @NullMarked
    public String toString() {
        return getClass().getName()
                + " ["
                + "Username="
                + getUsername()
                + ", "
                + "Enabled="
                + isEnabled()
                + ", "
                + "AdditionalParameters"
                + additionalParameters
                + "AccountNonExpired="
                + isAccountNonExpired()
                + ", "
                + "CredentialsNonExpired="
                + isCredentialsNonExpired()
                + ", "
                + "AccountNonLocked="
                + isAccountNonLocked()
                + ", "
                + "Granted Authorities="
                + getAuthorities()
                + "]";
    }
}
