package io.github.lishangbu.common.persistence.jimmer

import me.ahoo.cosid.IdGenerator
import org.babyfish.jimmer.sql.meta.UserIdGenerator
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.DependsOn

/**
 * 注册 Jimmer 使用的 CosId 长整型主键生成器。
 *
 * CosId 本身由官方 starter 负责自动装配。本配置只在官方共享雪花生成器
 * `__share__SnowflakeId` 已存在时建立 Jimmer 适配层，避免在 common 模块里维护
 * 另一套 CosId 属性和默认值。
 */
@AutoConfiguration(afterName = ["me.ahoo.cosid.spring.boot.starter.snowflake.CosIdSnowflakeAutoConfiguration"])
@ConditionalOnClass(IdGenerator::class, UserIdGenerator::class)
class CosIdJimmerAutoConfiguration {
	@Bean
	@DependsOn("snowflakeIdBeanRegistrar")
	@ConditionalOnProperty(prefix = "cosid.snowflake", name = ["enabled"], havingValue = "true")
	@ConditionalOnMissingBean(CosIdLongUserIdGenerator::class)
	fun cosIdLongUserIdGenerator(applicationContext: ApplicationContext): CosIdLongUserIdGenerator {
		val sharedSnowflakeId = applicationContext.getBean("__share__SnowflakeId", IdGenerator::class.java)
		return CosIdLongUserIdGenerator(sharedSnowflakeId)
	}
}
