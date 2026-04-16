package io.github.lishangbu.avalon.identity.access.application

/**
 * IdentityAccess 是 Avalon 的身份与访问上下文入口。
 *
 * 当前上下文拆成了两个主要子能力：
 * - `iam`：用户、角色、权限、菜单以及授权快照装配
 * - `authentication`：本地登录、refresh token、会话管理与认证日志
 *
 * 这里不负责：
 * - 完整 OAuth2 / OIDC 授权服务器平台能力
 * - 其他上下文的运行时业务写逻辑
 *
 * 认证链路当前采用短时效 JWT access token + 服务端保存 hash 的 opaque refresh token。
 */
object IdentityAccessModule