package io.github.lishangbu.avalon.oauth2.common.autoconfiguration

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder

/**
 * 密码编码器自动配置
 *
 * 在容器未提供自定义实现时注册默认的 [PasswordEncoder]
 */
@AutoConfiguration
class PasswordEncoderAutoConfiguration {
    /** 创建密码编码器 */
    @Bean
    @ConditionalOnMissingBean
    fun passwordEncoder(): PasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()
}
