package io.github.lishangbu.system.service

import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.security.entity.displayName
import io.github.lishangbu.security.entity.id
import io.github.lishangbu.security.entity.username
import io.github.lishangbu.security.rbac.JimmerSaTokenRbacService
import io.github.lishangbu.system.dto.SessionResponse
import io.github.lishangbu.system.dto.SessionRoleResponse
import io.github.lishangbu.system.dto.SessionUserResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 当前 Sa-Token 登录态查询服务。 */
@Service
class SessionService(
	private val rbacService: JimmerSaTokenRbacService,
) {
	/** 按 Sa-Token loginId 返回数据库中的实时角色和权限快照。 */
	@Transactional(readOnly = true)
	fun currentSession(userId: Long): SessionResponse {
		val user = rbacService.findUserById(userId) ?: notFound("userId", "用户不存在: $userId")
		val roles = rbacService.loadRoles(userId)
		val accessNodeCodes = rbacService.loadAccessNodes(userId).map { it.code }
		return SessionResponse(
			user = SessionUserResponse {
				id = user.id
				username = user.username
				displayName = user.displayName
			},
			roles = roles.map { role -> SessionRoleResponse(role.code, role.name) },
			accessNodeCodes = accessNodeCodes,
		)
	}
}
