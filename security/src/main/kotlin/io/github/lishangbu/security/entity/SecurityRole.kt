package io.github.lishangbu.security.entity

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.JoinTable
import org.babyfish.jimmer.sql.Key
import org.babyfish.jimmer.sql.ManyToMany
import org.babyfish.jimmer.sql.Table

/**
 * RBAC 角色定义。
 *
 * 角色用于聚合一组访问节点，用户登录后的最终授权仍以访问节点 code 为准。
 */
@Entity
@Table(name = "security_role")
interface SecurityRole {
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	val id: Long

	@Key
	val code: String

	val name: String

	/**
	 * 角色绑定的访问节点。
	 *
	 * 访问节点 code 是最终授权依据，角色只负责组织这些稳定访问点。
	 */
	@ManyToMany
	@JoinTable(
		name = "security_role_access_node",
		joinColumnName = "role_id",
		inverseJoinColumnName = "access_node_id",
	)
	val accessNodes: List<SecurityAccessNode>
}
