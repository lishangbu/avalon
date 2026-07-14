package io.github.lishangbu.common.web.security

import org.springframework.security.access.prepost.PreAuthorize

/** 仅允许执行战斗沙盒的主体调用的 API。 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@PreAuthorize("hasAuthority('$BATTLE_SANDBOX_RUN_AUTHORITY')")
annotation class RequireBattleSandboxRun
