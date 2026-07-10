# Security RBAC SAS Design

## 目标

后端作为 RBAC 专用服务运行，提供授权服务器、资源服务器、权限模型和管理 API。

## 模块边界

- `app`：Spring Boot 入口、CORS、运行时装配。
- `security`：Spring Authorization Server、资源服务器、认证主体、权限运行时和 SAS 基础设施。
- `system`：用户、角色、权限、OAuth client 和 JWK 系统管理 API。
- `migration`：安全与 OAuth schema 的 Liquibase changelog。
- `common-persistence`：Jimmer 与 CosId 共享基础设施。

## 权限模型

- 用户绑定角色。
- 角色绑定权限。
- 权限以稳定 `code` 作为后端和管理端共享契约。
- 默认权限：`security:admin`。
- 默认角色：`system-admin`。
- 默认管理员用户：`admin`。

## OAuth

默认 client：

- `system-admin-jwt`：签发 self-contained JWT access token。
- `system-admin-opaque`：签发 reference access token。

默认 scope：

```text
security:admin
```

自定义 password grant：

```text
grant_type=urn:security:params:oauth:grant-type:password&username=admin&password=123456&scope=security:admin
```

## API 权限

```text
/api/system/** -> security:admin
```

其他 `/api/**` 请求需要认证。后续新增业务 API 时必须显式决定权限 code，不直接读取安全表。

## 数据库

安全表由 Liquibase 管理：

- `security_user`
- `security_role`
- `security_permission`
- `security_user_role`
- `security_role_permission`
- `oauth2_client`
- `oauth2_authorization`
- `oauth2_authorization_consent`
- `oauth2_jwk`

新增迁移需要同时核对 Jimmer 实体、Repository、Service DTO 和 Testcontainers PostgreSQL 测试。

## 测试

- `migration` 验证空库迁移和默认 seed。
- `security` 验证 token endpoint、JWK 来源、OAuth client 运行时读取和用户详情服务。
- `system` 验证系统管理 API、DTO、校验和管理写入路径。
- `app` 验证运行时装配、CORS 和系统 API 访问控制。
