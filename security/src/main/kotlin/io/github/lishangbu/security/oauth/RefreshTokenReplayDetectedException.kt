package io.github.lishangbu.security.oauth

/** 在锁定旋转事务内发现旧 refresh token；携带 family ID 供事务回滚后独立撤销。 */
class RefreshTokenReplayDetectedException(val authorizationId: String) : RuntimeException()
