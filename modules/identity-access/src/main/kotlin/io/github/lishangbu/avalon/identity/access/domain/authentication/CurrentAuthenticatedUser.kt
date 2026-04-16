package io.github.lishangbu.avalon.identity.access.domain.authentication

import io.github.lishangbu.avalon.identity.access.domain.iam.AuthorizationSnapshot

/**
 * 面向“当前登录用户”接口返回的聚合视图。
 *
 * @property sessionId 当前访问会话标识。
 * @property snapshot 当前用户的完整授权快照。
 */
data class CurrentAuthenticatedUser(
    val sessionId: String,
    val snapshot: AuthorizationSnapshot,
)