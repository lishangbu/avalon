package io.github.lishangbu.avalon.authorization.service.impl;

import io.github.lishangbu.avalon.authorization.service.VerificationCodeService;
import io.github.lishangbu.avalon.oauth2.authorizationserver.exception.InvalidCaptchaException;
import io.github.lishangbu.avalon.oauth2.common.core.AuthorizationGrantTypeSupport;
import io.github.lishangbu.avalon.oauth2.common.properties.Oauth2Properties;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

/// 验证码服务抽象实现
///
/// 统一处理验证码生成、存储、频控与校验逻辑
///
/// @author lishangbu
/// @since 2026/3/13
public abstract class AbstractVerificationCodeService implements VerificationCodeService {

    private static final String CODE_KEY_PREFIX = "oauth2:verification:code:";
    private static final String RATE_LIMIT_KEY_PREFIX = "oauth2:verification:rate:";

    private static final int DEFAULT_CODE_LENGTH = 6;
    private static final Duration DEFAULT_CODE_TTL = Duration.ofMinutes(5);
    private static final Duration DEFAULT_RESEND_INTERVAL = Duration.ofSeconds(60);

    private final StringRedisTemplate stringRedisTemplate;
    private final Oauth2Properties oauth2Properties;

    protected AbstractVerificationCodeService(
            StringRedisTemplate stringRedisTemplate, Oauth2Properties oauth2Properties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.oauth2Properties = oauth2Properties;
    }

    @Override
    public String generateCode(String target, String type) {
        String normalizedType = normalizeType(type);
        String normalizedTarget = normalizeTarget(target, normalizedType);
        VerificationCodeConfig config = resolveConfig(normalizedType);

        enforceRateLimit(normalizedType, normalizedTarget, config.resendInterval());

        String code = generateNumericCode(config.length());
        String key = buildCodeKey(normalizedType, normalizedTarget);
        stringRedisTemplate.opsForValue().set(key, code, config.timeToLive());

        deliverCode(normalizedType, normalizedTarget, code);
        return code;
    }

    @Override
    public void verifyCode(String target, String code, String type) {
        String normalizedType = normalizeType(type);
        String normalizedTarget = normalizeTarget(target, normalizedType);
        String normalizedCode = normalizeCode(code);

        String key = buildCodeKey(normalizedType, normalizedTarget);
        String cachedCode = stringRedisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(cachedCode)) {
            throw new InvalidCaptchaException("验证码已过期或不存在");
        }
        if (!cachedCode.equals(normalizedCode)) {
            throw new InvalidCaptchaException("验证码错误");
        }
        stringRedisTemplate.delete(key);
    }

    /// 输出验证码（短信/邮件可由子类实现）
    protected abstract void deliverCode(String type, String target, String code);

    private void enforceRateLimit(String type, String target, Duration resendInterval) {
        if (resendInterval == null || resendInterval.isZero() || resendInterval.isNegative()) {
            return;
        }
        String key = buildRateLimitKey(type, target);
        Boolean allowed = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", resendInterval);
        if (allowed == null || !allowed) {
            throw new InvalidCaptchaException("验证码发送过于频繁，请稍后再试");
        }
    }

    private VerificationCodeConfig resolveConfig(String type) {
        if (AuthorizationGrantTypeSupport.SMS.getValue().equals(type)) {
            return new VerificationCodeConfig(
                    sanitizeLength(oauth2Properties.getSmsCodeLength(), DEFAULT_CODE_LENGTH),
                    resolveDuration(
                            oauth2Properties.getSmsCodeTimeToLiveDuration(), DEFAULT_CODE_TTL),
                    resolveDuration(
                            oauth2Properties.getSmsCodeResendIntervalDuration(),
                            DEFAULT_RESEND_INTERVAL));
        }
        if (AuthorizationGrantTypeSupport.EMAIL.getValue().equals(type)) {
            return new VerificationCodeConfig(
                    sanitizeLength(oauth2Properties.getEmailCodeLength(), DEFAULT_CODE_LENGTH),
                    resolveDuration(
                            oauth2Properties.getEmailCodeTimeToLiveDuration(), DEFAULT_CODE_TTL),
                    resolveDuration(
                            oauth2Properties.getEmailCodeResendIntervalDuration(),
                            DEFAULT_RESEND_INTERVAL));
        }
        throw new InvalidCaptchaException("不支持的验证码类型");
    }

    private String normalizeType(String type) {
        if (!StringUtils.hasText(type)) {
            throw new InvalidCaptchaException("验证码类型不能为空");
        }
        return type.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeTarget(String target, String type) {
        if (!StringUtils.hasText(target)) {
            throw new InvalidCaptchaException("验证码接收目标不能为空");
        }
        String trimmed = target.trim();
        if (AuthorizationGrantTypeSupport.EMAIL.getValue().equals(type)) {
            return trimmed.toLowerCase(Locale.ROOT);
        }
        return trimmed;
    }

    private String normalizeCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw new InvalidCaptchaException("验证码不能为空");
        }
        return code.trim();
    }

    private String buildCodeKey(String type, String target) {
        return CODE_KEY_PREFIX + type + ":" + target;
    }

    private String buildRateLimitKey(String type, String target) {
        return RATE_LIMIT_KEY_PREFIX + type + ":" + target;
    }

    private String generateNumericCode(int length) {
        int safeLength = Math.max(length, DEFAULT_CODE_LENGTH);
        int bound = (int) Math.pow(10, safeLength);
        int value = ThreadLocalRandom.current().nextInt(bound);
        return String.format(Locale.ROOT, "%0" + safeLength + "d", value);
    }

    private int sanitizeLength(Integer length, int defaultLength) {
        if (length == null || length <= 0) {
            return defaultLength;
        }
        return length;
    }

    private Duration resolveDuration(Duration configured, Duration fallback) {
        if (configured == null || configured.isNegative() || configured.isZero()) {
            return fallback;
        }
        return configured;
    }

    private record VerificationCodeConfig(
            int length, Duration timeToLive, Duration resendInterval) {}
}
