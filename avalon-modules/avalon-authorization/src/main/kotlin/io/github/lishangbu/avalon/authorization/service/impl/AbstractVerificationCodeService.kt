package io.github.lishangbu.avalon.authorization.service.impl

import io.github.lishangbu.avalon.authorization.service.VerificationCodeService
import io.github.lishangbu.avalon.oauth2.authorizationserver.exception.InvalidCaptchaException
import io.github.lishangbu.avalon.oauth2.common.core.AuthorizationGrantTypeSupport
import io.github.lishangbu.avalon.oauth2.common.properties.Oauth2Properties
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.Duration
import java.util.*
import java.util.concurrent.ThreadLocalRandom

/**
 * 验证码服务抽象实现
 *
 * 统一处理验证码生成、存储、频控与校验逻辑
 *
 * @author lishangbu
 * @since 2026/3/13
 */
abstract class AbstractVerificationCodeService(
    private val stringRedisTemplate: StringRedisTemplate,
    private val oauth2Properties: Oauth2Properties,
) : VerificationCodeService {
    override fun generateCode(
        target: String,
        type: String,
    ): String {
        val normalizedType = normalizeType(type)
        val normalizedTarget = normalizeTarget(target, normalizedType)
        val config = resolveConfig(normalizedType)

        enforceRateLimit(normalizedType, normalizedTarget, config.resendInterval)

        val code = generateNumericCode(config.length)
        val key = buildCodeKey(normalizedType, normalizedTarget)
        stringRedisTemplate.opsForValue().set(key, code, config.timeToLive)

        deliverCode(normalizedType, normalizedTarget, code)
        return code
    }

    override fun verifyCode(
        target: String,
        code: String,
        type: String,
    ) {
        val normalizedType = normalizeType(type)
        val normalizedTarget = normalizeTarget(target, normalizedType)
        val normalizedCode = normalizeCode(code)

        val key = buildCodeKey(normalizedType, normalizedTarget)
        val cachedCode = stringRedisTemplate.opsForValue().get(key)
        if (cachedCode.isNullOrBlank()) {
            throw InvalidCaptchaException("验证码已过期或不存在")
        }
        if (cachedCode != normalizedCode) {
            throw InvalidCaptchaException("验证码错误")
        }
        stringRedisTemplate.delete(key)
    }

    /** 输出验证码（短信/邮件可由子类实现） */
    protected abstract fun deliverCode(
        type: String,
        target: String,
        code: String,
    )

    private fun enforceRateLimit(
        type: String,
        target: String,
        resendInterval: Duration?,
    ) {
        if (resendInterval == null || resendInterval.isZero || resendInterval.isNegative) {
            return
        }
        val key = buildRateLimitKey(type, target)
        val allowed = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", resendInterval)
        if (allowed == null || !allowed) {
            throw InvalidCaptchaException("验证码发送过于频繁，请稍后再试")
        }
    }

    private fun resolveConfig(type: String): VerificationCodeConfig {
        if (AuthorizationGrantTypeSupport.SMS.value == type) {
            return VerificationCodeConfig(
                sanitizeLength(oauth2Properties.smsCodeLength, DEFAULT_CODE_LENGTH),
                resolveDuration(oauth2Properties.getSmsCodeTimeToLiveDuration(), DEFAULT_CODE_TTL),
                resolveDuration(
                    oauth2Properties.getSmsCodeResendIntervalDuration(),
                    DEFAULT_RESEND_INTERVAL,
                ),
            )
        }
        if (AuthorizationGrantTypeSupport.EMAIL.value == type) {
            return VerificationCodeConfig(
                sanitizeLength(oauth2Properties.emailCodeLength, DEFAULT_CODE_LENGTH),
                resolveDuration(
                    oauth2Properties.getEmailCodeTimeToLiveDuration(),
                    DEFAULT_CODE_TTL,
                ),
                resolveDuration(
                    oauth2Properties.getEmailCodeResendIntervalDuration(),
                    DEFAULT_RESEND_INTERVAL,
                ),
            )
        }
        throw InvalidCaptchaException("不支持的验证码类型")
    }

    private fun normalizeType(type: String): String {
        if (type.isBlank()) {
            throw InvalidCaptchaException("验证码类型不能为空")
        }
        return type.trim().lowercase(Locale.ROOT)
    }

    private fun normalizeTarget(
        target: String,
        type: String,
    ): String {
        if (target.isBlank()) {
            throw InvalidCaptchaException("验证码接收目标不能为空")
        }
        val trimmed = target.trim()
        if (AuthorizationGrantTypeSupport.EMAIL.value == type) {
            return trimmed.lowercase(Locale.ROOT)
        }
        return trimmed
    }

    private fun normalizeCode(code: String): String {
        if (code.isBlank()) {
            throw InvalidCaptchaException("验证码不能为空")
        }
        return code.trim()
    }

    private fun buildCodeKey(
        type: String,
        target: String,
    ): String = CODE_KEY_PREFIX + type + ":" + target

    private fun buildRateLimitKey(
        type: String,
        target: String,
    ): String = RATE_LIMIT_KEY_PREFIX + type + ":" + target

    private fun generateNumericCode(length: Int): String {
        val safeLength = maxOf(length, DEFAULT_CODE_LENGTH)
        return buildString(safeLength) {
            repeat(safeLength) {
                append(ThreadLocalRandom.current().nextInt(10))
            }
        }
    }

    private fun sanitizeLength(
        length: Int?,
        defaultLength: Int,
    ): Int {
        if (length == null || length <= 0) {
            return defaultLength
        }
        return length
    }

    private fun resolveDuration(
        configured: Duration?,
        fallback: Duration,
    ): Duration {
        if (configured == null || configured.isNegative || configured.isZero) {
            return fallback
        }
        return configured
    }

    private data class VerificationCodeConfig(
        val length: Int,
        val timeToLive: Duration,
        val resendInterval: Duration,
    )

    companion object {
        private const val CODE_KEY_PREFIX = "oauth2:verification:code:"
        private const val RATE_LIMIT_KEY_PREFIX = "oauth2:verification:rate:"
        private const val DEFAULT_CODE_LENGTH = 6
        private val DEFAULT_CODE_TTL: Duration = Duration.ofMinutes(5)
        private val DEFAULT_RESEND_INTERVAL: Duration = Duration.ofSeconds(60)
    }
}
