package io.github.lishangbu.security.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder

/** 配置账号密码摘要校验，算法前缀支持后续平滑升级。 */
@Configuration(proxyBeanMethods = false)
class PasswordConfig {
	@Bean
	fun passwordEncoder(): PasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()
}
