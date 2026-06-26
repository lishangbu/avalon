package io.github.lishangbu.security.rbac

import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Import

/**
 * RBAC 用户详情测试使用的最小 Spring Boot 应用。
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@Import(JimmerSecurityUserDetailsService::class)
class SecurityRbacTestApplication
