package io.github.lishangbu.security.config

import org.babyfish.jimmer.spring.repository.EnableJimmerRepositories
import org.springframework.context.annotation.Configuration

/** 注册安全领域的 Jimmer Repository。 */
@Configuration(proxyBeanMethods = false)
@EnableJimmerRepositories(basePackages = ["io.github.lishangbu.security.repository"])
class SecurityPersistenceConfig
