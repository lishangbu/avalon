package io.github.lishangbu.security.config

import cn.dev33.satoken.stp.StpUtil
import io.github.lishangbu.common.web.security.BATTLE_RULES_ADMIN_AUTHORITY
import io.github.lishangbu.common.web.security.BATTLE_SANDBOX_RUN_AUTHORITY
import io.github.lishangbu.common.web.security.BATTLE_SESSIONS_RUN_AUTHORITY
import io.github.lishangbu.common.web.security.GAME_DATA_ADMIN_AUTHORITY
import io.github.lishangbu.common.web.security.RequireBattleRulesAdmin
import io.github.lishangbu.common.web.security.RequireBattleSandboxRun
import io.github.lishangbu.common.web.security.RequireBattleSessionsRun
import io.github.lishangbu.common.web.security.RequireGameDataAdmin
import io.github.lishangbu.common.web.security.RequireSecurityAdmin
import io.github.lishangbu.common.web.security.SECURITY_ADMIN_AUTHORITY
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor
import kotlin.reflect.KClass

/** 将项目权限注解交给 Sa-Token 执行实时 RBAC 校验。 */
@Component
class ApiPermissionInterceptor : HandlerInterceptor {
	override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
		if (handler !is HandlerMethod) return true
		permissionRules.firstOrNull { (annotation, _) -> handler.hasAnnotation(annotation) }
			?.second
			?.let(StpUtil::checkPermission)
		return true
	}

	private fun HandlerMethod.hasAnnotation(annotation: KClass<out Annotation>): Boolean =
		AnnotatedElementUtils.hasAnnotation(beanType, annotation.java) ||
			AnnotatedElementUtils.hasAnnotation(method, annotation.java)

	private companion object {
		val permissionRules = listOf(
			RequireSecurityAdmin::class to SECURITY_ADMIN_AUTHORITY,
			RequireBattleRulesAdmin::class to BATTLE_RULES_ADMIN_AUTHORITY,
			RequireBattleSandboxRun::class to BATTLE_SANDBOX_RUN_AUTHORITY,
			RequireBattleSessionsRun::class to BATTLE_SESSIONS_RUN_AUTHORITY,
			RequireGameDataAdmin::class to GAME_DATA_ADMIN_AUTHORITY,
		)
	}
}
