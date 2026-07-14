package io.github.lishangbu.battlerules.openapi

import io.github.lishangbu.common.web.openapi.OPENAPI_TOKEN_SECURITY_SCHEME

const val BATTLE_RULES_API_BEARER_AUTH = OPENAPI_TOKEN_SECURITY_SCHEME
const val BATTLE_SANDBOX_API_BEARER_AUTH = OPENAPI_TOKEN_SECURITY_SCHEME
const val BATTLE_SESSIONS_API_BEARER_AUTH = OPENAPI_TOKEN_SECURITY_SCHEME
const val BATTLE_SESSIONS_API_SCOPE = "battle-sessions:run"
const val BATTLE_SESSIONS_API_BAD_REQUEST_DESCRIPTION = "请求参数、请求体或业务校验未通过。"
const val BATTLE_SESSIONS_API_UNAUTHORIZED_DESCRIPTION = "未提供 avalon-token，或登录已过期。"
const val BATTLE_SESSIONS_API_FORBIDDEN_DESCRIPTION = "登录有效，但缺少 battle-sessions:run 权限。"
const val BATTLE_SESSIONS_API_NOT_FOUND_DESCRIPTION = "Battle Session 不存在或已被 Runtime 淘汰。"
const val BATTLE_SESSIONS_API_CONFLICT_DESCRIPTION = "revision 过期、commandId 负载冲突或 Session 已进入终态。"
const val BATTLE_RULES_API_BAD_REQUEST_DESCRIPTION = "请求参数、请求体或业务校验未通过；响应体会给出稳定错误码、中文说明和可选字段名。"
const val BATTLE_RULES_API_UNAUTHORIZED_DESCRIPTION = "未提供 avalon-token，或登录已过期。"
const val BATTLE_RULES_API_FORBIDDEN_DESCRIPTION = "登录有效，但缺少访问战斗规则 API 所需的 battle-rules:admin 权限。"
const val BATTLE_RULES_API_NOT_FOUND_DESCRIPTION = "指定战斗规则资料不存在，或已不在当前维护边界内。"
const val BATTLE_RULES_API_CONFLICT_DESCRIPTION = "请求与现有规则资料存在唯一性、引用或并发冲突。"
