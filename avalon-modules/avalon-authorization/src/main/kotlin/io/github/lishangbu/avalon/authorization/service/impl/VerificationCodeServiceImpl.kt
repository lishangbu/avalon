package io.github.lishangbu.avalon.authorization.service.impl

import io.github.lishangbu.avalon.oauth2.common.properties.Oauth2Properties
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service

/**
 * 验证码服务实现
 *
 * 使用 Redis 存储验证码，并通过日志输出短信与邮件内容
 *
 * @author lishangbu
 * @since 2026/3/13
 */
@Service
class VerificationCodeServiceImpl(
    stringRedisTemplate: StringRedisTemplate,
    oauth2Properties: Oauth2Properties,
) : AbstractVerificationCodeService(stringRedisTemplate, oauth2Properties) {
    override fun deliverCode(
        type: String,
        target: String,
        code: String,
    ) {
        log.info("发送验证码 -> type={}, target={}, code={}", type, target, code)
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(VerificationCodeServiceImpl::class.java)
    }
}
