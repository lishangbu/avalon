package io.github.lishangbu.security.entity

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Key
import org.babyfish.jimmer.sql.Table

/**
 * 系统访问节点定义。
 *
 * 访问节点同时承载管理端菜单、路由动作和后端 API 授权点，`code` 是角色绑定和运行时鉴权的稳定依据。
 */
@Entity
@Table(name = "security_access_node")
interface SecurityAccessNode {
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	val id: Long

	@Key
	val code: String

	val name: String

	val type: String

	val parentId: Long?

	val path: String?

	val componentKey: String?

	val icon: String?

	val sortOrder: Int

	val visible: Boolean

	val enabled: Boolean

	val apiMethod: String?

	val apiPattern: String?
}
