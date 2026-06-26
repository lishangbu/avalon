package io.github.lishangbu.common.persistence.jimmer

import me.ahoo.cosid.IdGenerator
import org.babyfish.jimmer.sql.meta.UserIdGenerator

/**
 * 将 CosId 的长整型生成器适配为 Jimmer 的主键生成器。
 *
 * 该类型只负责桥接两个框架的接口，不创建或配置 CosId 实例；实际生成器由官方
 * `cosid-spring-boot-starter` 按 `cosid.*` 配置注册。
 */
class CosIdLongUserIdGenerator(
	private val idGenerator: IdGenerator,
) : UserIdGenerator<Long> {
	override fun generate(entityType: Class<*>): Long =
		idGenerator.generate()
}
