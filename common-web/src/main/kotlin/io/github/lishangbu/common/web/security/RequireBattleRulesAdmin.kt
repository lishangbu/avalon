package io.github.lishangbu.common.web.security


/** 仅允许战斗规则管理员调用的 API。 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class RequireBattleRulesAdmin
