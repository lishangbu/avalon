package io.github.lishangbu.system.openapi

import io.github.lishangbu.common.web.openapi.OPENAPI_TOKEN_SECURITY_SCHEME

const val SYSTEM_API_BEARER_AUTH = OPENAPI_TOKEN_SECURITY_SCHEME
const val SYSTEM_API_UNAUTHORIZED_DESCRIPTION = "未提供 avalon-token，或登录已过期。"
const val SYSTEM_API_FORBIDDEN_DESCRIPTION = "登录有效，但缺少访问系统管理 API 所需的 security:admin 权限。"
const val SYSTEM_API_BAD_REQUEST_DESCRIPTION = "请求参数、请求体或业务校验未通过；响应体会给出稳定错误码、中文说明和可选字段名。"
const val SYSTEM_API_NOT_FOUND_DESCRIPTION = "指定资源不存在，或资源已不在当前管理边界内。"
const val SYSTEM_API_CONFLICT_DESCRIPTION = "请求与现有资源存在唯一性、状态或并发冲突。"
