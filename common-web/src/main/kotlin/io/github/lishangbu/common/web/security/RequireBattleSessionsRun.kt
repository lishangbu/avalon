package io.github.lishangbu.common.web.security

import org.springframework.security.access.prepost.PreAuthorize

/** 仅允许执行战斗会话的主体调用的 API。 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@PreAuthorize("hasAuthority('$BATTLE_SESSIONS_RUN_AUTHORITY')")
annotation class RequireBattleSessionsRun
