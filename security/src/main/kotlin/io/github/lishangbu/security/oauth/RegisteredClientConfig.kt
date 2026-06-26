package io.github.lishangbu.security.oauth

import io.github.lishangbu.security.repository.JimmerRegisteredClientRepository
import io.github.lishangbu.security.repository.OAuth2ClientRepository
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository

/**
 * 装配授权服务器客户端仓库。
 *
 * 客户端正式数据由 Liquibase seed 或后续管理端写入数据库，启动阶段不再从配置文件
 * 临时创建 client。
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "backend.security", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class RegisteredClientConfig {
	@Bean
	fun registeredClientRepository(
		clientRepository: OAuth2ClientRepository,
		sqlClient: KSqlClient,
	): RegisteredClientRepository =
		JimmerRegisteredClientRepository(clientRepository, sqlClient)
}
