package io.github.lishangbu.security.repository

import io.github.lishangbu.security.entity.SecurityUser
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * RBAC 用户账号的 Jimmer Repository。
 */
interface SecurityUserRepository : KRepository<SecurityUser, Long>
