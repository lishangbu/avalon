package io.github.lishangbu.common.web.security


/** 仅允许系统管理员调用的 API。 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class RequireSecurityAdmin
