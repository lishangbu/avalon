package io.github.lishangbu.security.repository

import io.github.lishangbu.security.entity.SecurityAdminAudit
import org.babyfish.jimmer.spring.repository.KRepository

interface SecurityAdminAuditRepository : KRepository<SecurityAdminAudit, Long>
