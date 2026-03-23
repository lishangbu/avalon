package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.AuthenticationLog

/**
 * 认证日志仓储接口
 *
 * 定义认证日志数据的查询与持久化操作
 */
interface AuthenticationLogRepository {
    /** 保存认证日志 */
    fun save(authenticationLog: AuthenticationLog): AuthenticationLog

    /** 保存认证日志并立即刷新 */
    fun saveAndFlush(authenticationLog: AuthenticationLog): AuthenticationLog
}
