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
    /** String Redis 模板 */
    private val stringRedisTemplate: StringRedisTemplate,
    /** OAuth2 属性 */
    private val oauth2Properties: Oauth2Properties,
) : VerificationCodeService {
    /** 生成状态码 */
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

    /** 返回校验状态码 */
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

    /** 返回发送状态码 */
    protected abstract fun deliverCode(
        type: String,
        target: String,
        code: String,
    )

    /** 返回执行速率限制 */
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

    /** 解析配置 */
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

    /** 规范化属性 */
    private fun normalizeType(type: String): String {
        if (type.isBlank()) {
            throw InvalidCaptchaException("验证码类型不能为空")
        }
        return type.trim().lowercase(Locale.ROOT)
    }

    /** 规范化目标 */
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

    /** 规范化状态码 */
    private fun normalizeCode(code: String): String {
        if (code.isBlank()) {
            throw InvalidCaptchaException("验证码不能为空")
        }
        return code.trim()
    }

    /** 构建状态码密钥 */
    private fun buildCodeKey(
        type: String,
        target: String,
    ): String = CODE_KEY_PREFIX + type + ":" + target

    /** 构建速率限制密钥 */
    private fun buildRateLimitKey(
        type: String,
        target: String,
    ): String = RATE_LIMIT_KEY_PREFIX + type + ":" + target

    /** 生成数字验证码 */
    private fun generateNumericCode(length: Int): String {
        val safeLength = maxOf(length, DEFAULT_CODE_LENGTH)
        return buildString(safeLength) {
            repeat(safeLength) {
                append(ThreadLocalRandom.current().nextInt(10))
            }
        }
    }

    /** 返回清理长度 */
    private fun sanitizeLength(
        length: Int?,
        defaultLength: Int,
    ): Int {
        if (length == null || length <= 0) {
            return defaultLength
        }
        return length
    }

    /** 解析时长 */
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
        /** 长度 */
        val length: Int,
        /** 有效期 */
        val timeToLive: Duration,
        /** 重发间隔 */
        val resendInterval: Duration,
    )

    companion object {
        /** 验证码键前缀 */
        private const val CODE_KEY_PREFIX = "oauth2:verification:code:"

        /** 速率限制键前缀 */
        private const val RATE_LIMIT_KEY_PREFIX = "oauth2:verification:rate:"

        /** 默认状态码长度 */
        private const val DEFAULT_CODE_LENGTH = 6

        /** 默认验证码 TTL */
        private val DEFAULT_CODE_TTL: Duration = Duration.ofMinutes(5)

        /** 默认重发间隔 */
        private val DEFAULT_RESEND_INTERVAL: Duration = Duration.ofSeconds(60)
    }
}
