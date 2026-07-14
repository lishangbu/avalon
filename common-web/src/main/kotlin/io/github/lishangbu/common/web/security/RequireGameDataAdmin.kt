package io.github.lishangbu.common.web.security


/** 仅允许游戏资料管理员调用的 API。 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class RequireGameDataAdmin
