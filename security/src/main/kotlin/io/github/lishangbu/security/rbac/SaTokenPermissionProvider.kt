package io.github.lishangbu.security.rbac

import cn.dev33.satoken.stp.StpInterface
import org.springframework.stereotype.Component

/** 从 Jimmer RBAC 关系提供 Sa-Token 角色和权限列表。 */
@Component
class SaTokenPermissionProvider(
	private val rbacService: JimmerSaTokenRbacService,
) : StpInterface {
	override fun getPermissionList(loginId: Any, loginType: String): List<String> =
		rbacService.loadAccessNodes(loginId.toString().toLong()).map(UserAccessNode::code)

	override fun getRoleList(loginId: Any, loginType: String): List<String> =
		rbacService.loadRoles(loginId.toString().toLong()).map(UserRole::code)
}
