package io.github.lishangbu.security.entity

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.JoinTable
import org.babyfish.jimmer.sql.Key
import org.babyfish.jimmer.sql.ManyToMany
import org.babyfish.jimmer.sql.Table

/**
 * RBAC 用户账号。
 *
 * 该实体只保存认证所需的账号状态和密码摘要，角色绑定由 `security_user_role`
 * 关联表维护，避免把认证读取路径绑定到复杂对象图。
 */
@Entity
@Table(name = "security_user")
interface SecurityUser {
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	@JsonConverter(LongToStringConverter::class)
	val id: Long

	@Key
	val username: String

	val passwordHash: String
	val displayName: String
	val enabled: Boolean
	val accountNonLocked: Boolean

	/**
	 * 用户绑定的 RBAC 角色。
	 *
	 * 关联只用于认证快照和系统管理查询，不把角色生命周期合并进用户聚合。
	 */
	@ManyToMany
	@JoinTable(
		name = "security_user_role",
		joinColumnName = "user_id",
		inverseJoinColumnName = "role_id",
	)
	val roles: List<SecurityRole>
}
