package io.github.lishangbu.common.web.security


/** 仅允许执行战斗沙盒的主体调用的 API。 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class RequireBattleSandboxRun
