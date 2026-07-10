---
name: spring-security-oauth2
description: "Spring Security and OAuth2 work in Avalon. Use for Spring Authorization Server, resource server configuration, authentication, password grant behavior, tokens, sessions, users, roles, permissions, OAuth clients, JWKs, method authorization, security filters, or authorization tests."
---

# Spring Security 与 OAuth2

## 修改前

- 先定位授权服务器、资源服务器、认证主体、权限查询与管理 API 的拥有边界。
- 涉及持久化、Web、JSON 或 OpenAPI 时同时加载相应技术栈 skill。
- 修改稳定 permission code 或身份语义时读取 `CONTEXT.md` 与相关 ADR。

## 项目要求

- 后端是授权权威；前端隐藏按钮不能替代后端拒绝。
- 用户绑定角色、角色绑定稳定 permission code；正式权限由 Liquibase seed 管理，不在请求时临时生成。
- 管理 API 的读取和写入均执行权限校验；超级管理员或系统任务的绕过路径必须显式、最小且可测试。
- 密码只保存强哈希；token、client secret、私钥和 JWK 私有材料不得写日志或错误响应。
- OAuth client、token settings 与 client settings 使用明确列映射，不用含糊 JSON 默认包。
- 401、403、无效 client、过期 token 与撤销 token 使用稳定且不泄密的响应。
- 安全 bean 使用一致条件装配；禁用安全功能时不得留下半启用 filter chain。
- 不以 Controller 路径或前端菜单作为唯一权限事实。

## 测试驱动

- 先写未认证、权限不足和授权成功三类失败/成功测试。
- token 生命周期覆盖签发、刷新或自定义 grant、撤销、过期和密钥轮换相关边界。
- 数据库权限变化使用 PostgreSQL 集成测试，filter chain 使用 Spring Security 测试支持。

## 完成标准

- 安全配置与管理 API 集成测试通过。
- 每个新端点都有明确认证与授权断言。
- 日志、错误、DTO 和 OpenAPI 不暴露秘密字段。
- permission code、Liquibase seed、后端表达式和前端 Contract Snapshot 可对应。
