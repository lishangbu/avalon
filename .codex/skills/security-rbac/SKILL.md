---
name: security-rbac
description: "Use when working on avalon security and RBAC: Spring Authorization Server, roles, permissions, users, clients, tokens, method authorization, route permissions, security schema, and authorization tests."
---

# 授权与 RBAC

## 使用范围

用于 Spring Authorization Server、登录授权、用户、角色、权限、客户端、token、接口鉴权、系统管理 API 和 RBAC 测试。

## 模块边界

- `security` 拥有安全实体、权限模型、授权配置、认证主体和安全运行时测试。
- `system` 依赖 `security`，承接用户、角色、权限、OAuth client、JWK 等系统管理 API、DTO、校验和管理测试。
- `app` 装配安全模块和系统管理模块。
- 当前后端聚焦 RBAC，不装配 catalog、dataset import 或 battle 模块。
- 新业务模块如果以后恢复，应通过边界注解、权限表达式或共享契约接入授权，不直接读取安全表。

## RBAC 模型

- 用户绑定角色。
- 角色绑定权限。
- 权限使用稳定 code 表达。
- API 和前端路由都基于权限 code 判断。
- 超级管理员或系统任务必须有清晰绕过路径。

## 授权原则

- 后端强制鉴权，前端只改善交互体验。
- 管理端无权限动作应禁用或隐藏，但后端仍必须拒绝。
- 系统管理 API 使用 `/api/system/**` 路径，并由 `security:admin` 权限保护。
- 错误响应不泄露 token、密钥、SQL 或内部栈。
- 安全配置变化必须有集成测试。

## 数据库

- 安全表结构通过 Liquibase 管理。
- 权限 code 和内置角色属于稳定契约。
- 不通过运行时临时代码创建正式权限数据。
