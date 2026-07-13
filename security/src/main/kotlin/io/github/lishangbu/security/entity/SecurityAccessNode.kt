package io.github.lishangbu.security.entity

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Key
import org.babyfish.jimmer.sql.Table

/**
 * 系统访问权限定义。
 *
 * 访问权限只描述可被角色授予的稳定能力；菜单、路由和接口路径分别由各客户端与代码授权规则维护。
 */
@Entity
@Table(name = "security_access_node")
interface SecurityAccessNode {
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	@JsonConverter(LongToStringConverter::class)
	val id: Long

	@Key
	val code: String

	val name: String

	val enabled: Boolean
}
