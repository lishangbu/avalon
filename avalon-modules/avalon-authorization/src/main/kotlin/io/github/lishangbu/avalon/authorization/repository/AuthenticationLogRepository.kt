package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.entity.AuthenticationLog

interface AuthenticationLogRepository {
    fun save(authenticationLog: AuthenticationLog): AuthenticationLog

    fun saveAndFlush(authenticationLog: AuthenticationLog): AuthenticationLog
}
