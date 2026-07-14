package io.github.lishangbu.gamedata.openapi

import io.github.lishangbu.common.web.openapi.OPENAPI_TOKEN_SECURITY_SCHEME

const val GAME_DATA_API_BEARER_AUTH = OPENAPI_TOKEN_SECURITY_SCHEME
const val GAME_DATA_API_BAD_REQUEST_DESCRIPTION = "请求参数不正确"
const val GAME_DATA_API_UNAUTHORIZED_DESCRIPTION = "未认证或登录态已过期"
const val GAME_DATA_API_FORBIDDEN_DESCRIPTION = "没有游戏资料管理权限"
const val GAME_DATA_API_NOT_FOUND_DESCRIPTION = "资料不存在"
const val GAME_DATA_API_CONFLICT_DESCRIPTION = "资料存在唯一约束冲突或仍被其他资料引用"
