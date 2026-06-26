package io.github.lishangbu.security.repository

import io.github.lishangbu.security.entity.SecurityRole
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * RBAC 角色定义的 Jimmer Repository。
 */
interface SecurityRoleRepository : KRepository<SecurityRole, Long>
