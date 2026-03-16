package io.github.lishangbu.avalon.authorization.service.impl;

import io.github.lishangbu.avalon.oauth2.common.properties.Oauth2Properties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/// 验证码服务实现
///
/// 使用 Redis 存储验证码，并通过日志输出短信与邮件内容
///
/// @author lishangbu
/// @since 2026/3/13
@Slf4j
@Service
public class VerificationCodeServiceImpl extends AbstractVerificationCodeService {

    public VerificationCodeServiceImpl(
            StringRedisTemplate stringRedisTemplate, Oauth2Properties oauth2Properties) {
        super(stringRedisTemplate, oauth2Properties);
    }

    @Override
    protected void deliverCode(String type, String target, String code) {
        log.info("发送验证码 -> type={}, target={}, code={}", type, target, code);
    }
}
