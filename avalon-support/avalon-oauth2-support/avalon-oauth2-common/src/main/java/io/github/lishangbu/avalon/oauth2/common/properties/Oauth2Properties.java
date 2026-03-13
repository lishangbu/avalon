package io.github.lishangbu.avalon.oauth2.common.properties;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationStyle;

/// OAuth2 安全配置属性类
///
/// 封装 OAuth2 授权服务器的核心配置参数，支持通过 application.yml 或 application.properties 进行配置
/// 前缀为 `oauth2`，包含认证路径控制、JWT 密钥管理、Token 签发等关键设置
///
/// @author lishangbu
/// @since 2025/8/17
@Data
@AutoConfiguration
@ConfigurationProperties(prefix = Oauth2Properties.PREFIX)
public class Oauth2Properties {
    /// 配置属性前缀，用于绑定 application.yml 中的 oauth2 配置段
    public static final String PREFIX = "oauth2";

    /// 免认证路径列表，匹配的请求路径将跳过 OAuth2 安全验证，支持 Ant 模式
    private List<String> ignoreUrls = new ArrayList<>();

    /// 用户名参数名称，用于密码授权模式中的用户名字段
    private String usernameParameterName = "username";

    /// 密码参数名称，用于密码授权模式中的密码字段
    private String passwordParameterName = "password";

    /// 手机号参数名称，用于短信授权模式中的手机号字段
    private String phoneParameterName = "phone";

    /// 短信验证码参数名称，用于短信授权模式中的验证码字段
    private String smsCodeParameterName = "sms_code";

    /// 邮箱参数名称，用于邮箱授权模式中的邮箱字段
    private String emailParameterName = "email";

    /// 邮箱验证码参数名称，用于邮箱授权模式中的验证码字段
    private String emailCodeParameterName = "email_code";

    /// 登录失败允许的最大尝试次数，<=0 则禁用
    private Integer maxLoginFailures = 5;

    /// 登录失败后的封禁时长，<=0 则禁用
    private String loginLockDuration = "15m";

    public Duration getLoginLockDuration() {
        if (loginLockDuration == null || loginLockDuration.isBlank()) {
            return null;
        }
        try {
            return DurationStyle.detectAndParse(loginLockDuration.trim(), ChronoUnit.MINUTES);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public void setLoginLockDuration(String loginLockDuration) {
        this.loginLockDuration = loginLockDuration;
    }

    /// JWT Token 签发者地址，用于标识 Token 的颁发机构
    private String issuerUrl;

    /// JWT 公钥文件位置，用于 Token 签名验证
    private String jwtPublicKeyLocation;

    /// JWT 私钥文件位置，用于 Token 签名生成
    private String jwtPrivateKeyLocation;
}
