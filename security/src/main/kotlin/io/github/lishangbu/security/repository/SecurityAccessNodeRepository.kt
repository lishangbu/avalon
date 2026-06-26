package io.github.lishangbu.security.repository

import io.github.lishangbu.security.entity.SecurityAccessNode
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 系统访问节点的 Jimmer Repository。
 */
interface SecurityAccessNodeRepository : KRepository<SecurityAccessNode, Long>
