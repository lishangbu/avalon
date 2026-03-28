package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.AuthenticationLog
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 认证日志仓储接口
 *
 * 定义认证日志数据的查询与持久化操作
 */
interface AuthenticationLogRepository : KRepository<AuthenticationLog, Long>
