# Security RBAC SAS Foundation

## 当前目标

后端聚焦 Spring Authorization Server 与 RBAC 管理能力，不再装配目录、数据导入或战斗模块。

保留模块：

- `app`
- `common-persistence`
- `migration`
- `security`
- `system`

## 范围

- `security` 拥有安全实体、Repository、认证主体、授权服务器、资源服务器和 token/JWK 运行时能力。
- `system` 拥有用户、角色、权限、OAuth client 和 JWK 等系统管理 API、DTO、校验和管理服务。
- `migration` 只保留安全、OAuth 和 RBAC schema，并通过新增迁移清理旧业务表。
- `app` 只负责运行时装配、CORS 和应用启动。
- 默认内置权限只保留 `security:admin`。
- 默认内置角色只保留 `system-admin`。
- 默认 OAuth client 只授予 `security:admin` scope。

## 后续任务

1. 强化 OAuth client 管理 API 的字段校验和错误模型。
2. 为 RBAC 权限、角色、用户管理 API 补充分页和筛选。
3. 增加权限 code 的稳定契约测试。
4. 增加 JWK 轮换的并发与回滚测试。
5. 根据管理端需要补齐只读查询 DTO。

## 验证命令

```bash
./gradlew :migration:test
./gradlew :security:test
./gradlew :system:test
./gradlew :app:test
./gradlew test
```
