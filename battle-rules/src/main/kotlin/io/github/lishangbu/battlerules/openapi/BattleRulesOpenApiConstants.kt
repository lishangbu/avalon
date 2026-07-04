package io.github.lishangbu.battlerules.openapi

import io.github.lishangbu.common.web.openapi.OPENAPI_BEARER_SECURITY_SCHEME

const val BATTLE_RULES_API_BEARER_AUTH = OPENAPI_BEARER_SECURITY_SCHEME
const val BATTLE_SANDBOX_API_BEARER_AUTH = OPENAPI_BEARER_SECURITY_SCHEME
const val BATTLE_RULES_API_BAD_REQUEST_DESCRIPTION = "请求参数、请求体或业务校验未通过；响应体会给出稳定错误码、中文说明和可选字段名。"
const val BATTLE_RULES_API_UNAUTHORIZED_DESCRIPTION = "未提供 Bearer access token，或 access token 已过期、格式错误、无法通过资源服务器校验。"
const val BATTLE_RULES_API_FORBIDDEN_DESCRIPTION = "access token 有效，但缺少访问战斗规则 API 所需的 battle-rules:admin 权限。"
const val BATTLE_RULES_API_NOT_FOUND_DESCRIPTION = "指定战斗规则资料不存在，或已不在当前维护边界内。"
const val BATTLE_RULES_API_CONFLICT_DESCRIPTION = "请求与现有规则资料存在唯一性、引用或并发冲突。"
