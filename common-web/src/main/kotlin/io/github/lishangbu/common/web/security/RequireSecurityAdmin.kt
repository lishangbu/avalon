package io.github.lishangbu.common.web.security

import org.springframework.security.access.prepost.PreAuthorize

/** 仅允许系统管理员调用的 API。 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@PreAuthorize("hasAuthority('$SECURITY_ADMIN_AUTHORITY')")
annotation class RequireSecurityAdmin
