# Avalon

![Kotlin](https://img.shields.io/badge/Kotlin-2.3.21-7F52FF?logo=kotlin&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-6DB33F?logo=springboot&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-9.5.1-02303A?logo=gradle&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-required-4169E1?logo=postgresql&logoColor=white)
![Jimmer](https://img.shields.io/badge/Jimmer-0.10.11-2F7D32)
![CosId](https://img.shields.io/badge/CosId-3.1.0-0A66C2)
![Liquibase](https://img.shields.io/badge/Liquibase-migration-2962FF)
![Spring Authorization Server](https://img.shields.io/badge/Spring%20Authorization%20Server-enabled-6DB33F?logo=springsecurity&logoColor=white)
![Quartz](https://img.shields.io/badge/Quartz-scheduler-4A5568)
![AWS SDK v2 S3](https://img.shields.io/badge/AWS%20SDK%20v2-S3-FF9900?logo=amazonaws&logoColor=white)
![Testcontainers](https://img.shields.io/badge/Testcontainers-2.0.5-2496ED?logo=docker&logoColor=white)
![License](https://img.shields.io/badge/License-AGPL--3.0-blue)

Avalon 是面向后台管理场景的 Kotlin Spring Boot 后端服务，提供 RBAC、OAuth2 授权、系统管理、定时任务和 S3 对象存储基础能力。管理端前端仓库请参考 [avalon-admin-ui](https://github.com/lishangbu/avalon-admin-ui)。

## 导航

[管理端仓库](https://github.com/lishangbu/avalon-admin-ui) | [S3 Starter](./s3-spring-boot-starter/README.md) | [安全与 RBAC 设计](./docs/superpowers/specs/2026-06-22-security-rbac-sas-design.md)

## 功能模块

| 模块 | 说明 |
| --- | --- |
| `app` | Spring Boot 应用入口，负责运行时装配、Web 配置和启动。 |
| `common-web` | 共享 Web 基础设施，包含分页请求、错误响应和全局异常处理。 |
| `common-persistence` | 共享持久化基础设施，包含 Jimmer 与 CosId 集成。 |
| `migration` | Liquibase 数据库变更与迁移验证。 |
| `security` | Spring Authorization Server、资源服务器、RBAC 运行时、OAuth2 client 与 JWK 持久化。 |
| `system` | 系统管理 API，覆盖用户、角色、权限、OAuth client、JWK 和定时任务管理。 |
| `scheduler` | 基于 Quartz 的可管理定时任务能力，包含任务注册、调度、执行记录和持久化。 |
| `game-data` | 游戏资料管理领域，维护生物、技能、道具、属性、地区等基础资料。 |
| `battle-engine` | 战斗引擎核心，覆盖现代主系列规则、事件流程和公开规则用例测试。 |
| `battle-rules` | 战斗规则维护 API 和运行时规则快照，供管理端和战斗引擎读取。 |
| `s3-core` | S3 操作抽象、对象键、命令对象和 AWS SDK v2 适配。 |
| `s3-spring-boot-autoconfigure` | S3 Spring Boot 自动配置。 |
| `s3-spring-boot-starter` | 对外使用的 S3 starter 依赖入口。 |

## 快速开始

### 环境要求

- JDK 25
- PostgreSQL
- Docker（运行 Testcontainers 测试时需要）
- 本仓库自带的 Gradle Wrapper

默认数据库连接见 [application.yaml](./app/src/main/resources/application.yaml)：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/avalon
    username: postgres
    password: postgres
```

### 启动后端

```bash
./gradlew :app:bootRun
```

Windows PowerShell 可使用：

```powershell
.\gradlew.bat :app:bootRun
```

默认访问地址：

- 后端服务：http://localhost:8080
- API 基础路径：http://localhost:8080/api
- 默认允许的管理端来源：http://localhost:5173

### 前后端联调

1. 启动 PostgreSQL，并创建 `avalon` 数据库。
2. 启动本仓库后端服务。
3. 启动 [avalon-admin-ui](https://github.com/lishangbu/avalon-admin-ui) 管理端。
4. 确认前端访问来源为 `http://localhost:5173`，后端默认 CORS 配置已允许该来源。

如需覆盖安全发行方地址，可设置环境变量：

```bash
AVALON_SECURITY_ISSUER=http://localhost:8080
```

CosId 机器号可通过环境变量设置：

```bash
COSID_MACHINE_ID=0
```

## API 分组

| 分组 | 路径 |
| --- | --- |
| 当前会话 | `/api/session` |
| 用户管理 | `/api/system/rbac/users` |
| 角色管理 | `/api/system/rbac/roles` |
| 权限查询 | `/api/system/rbac/permissions` |
| 访问节点查询 | `/api/system/rbac/access-nodes` |
| OAuth client 管理 | `/api/system/oauth/clients` |
| OAuth token 管理 | `/api/system/oauth/tokens` |
| JWK 管理 | `/api/system/oauth/jwks` |
| 定时任务管理 | `/api/system/scheduler/tasks` |
| 游戏资料管理 | `/api/game-data/**` |
| 战斗规则管理 | `/api/battle-rules/**` |

OAuth2 Token 端点由 Spring Authorization Server 提供，使用当前后端的安全配置和持久化客户端数据。

## 常用命令

```bash
./gradlew help
./gradlew projects
./gradlew test
./gradlew clean test
./gradlew :app:bootRun
```

按模块运行测试：

```bash
./gradlew :app:test
./gradlew :common-web:test
./gradlew :common-persistence:test
./gradlew :game-data:test
./gradlew :battle-engine:test
./gradlew :battle-rules:test
./gradlew :migration:test
./gradlew :scheduler:test
./gradlew :security:test
./gradlew :system:test
./gradlew :s3-core:test
./gradlew :s3-spring-boot-autoconfigure:test
```

### 验证节奏

根据改动范围选择最小可覆盖的验证命令；准备提交或合并前再跑全量测试。

| 改动范围 | 建议命令 |
| --- | --- |
| 管理端登录态、菜单、RBAC、OAuth、OpenAPI 或应用装配 | `./gradlew :app:test --tests '*SecurityManagementApiTests' --tests '*OpenApiDocumentationTests'` |
| Liquibase 表结构、注释或初始化数据 | `./gradlew :migration:test` |
| 游戏资料领域服务或持久层 | `./gradlew :game-data:test` |
| 战斗规则维护 API、规则快照或规则持久层 | `./gradlew :battle-rules:test` |
| 战斗引擎规则、事件流程或公开规则用例 | `./gradlew :battle-engine:test` |
| 只检查 312 条规则覆盖台账 | `./gradlew :battle-engine:test --tests '*BattleRuleCoverageLedgerTests'` |
| 跨模块重构或发布前回归 | `./gradlew test` |

发布 S3 starter 到本地 Maven 仓库：

```bash
./gradlew :s3-core:publishToMavenLocal \
    :s3-spring-boot-autoconfigure:publishToMavenLocal \
    :s3-spring-boot-starter:publishToMavenLocal
```

## 项目文档

- [S3 Spring Boot Starter](./s3-spring-boot-starter/README.md)
- [安全与 RBAC 设计](./docs/superpowers/specs/2026-06-22-security-rbac-sas-design.md)
- [安全与 RBAC 实施计划](./docs/superpowers/plans/2026-06-22-security-rbac-sas-foundation.md)
- [后端基础实施计划](./docs/superpowers/plans/2026-06-20-backend-foundation.md)

## 贡献

欢迎提交 Issue 和 Pull Request。提交改动前建议至少运行与改动范围匹配的模块测试；涉及数据库结构时，同时运行 `:migration:test`。

## 维护者

[ShangBu Li](https://github.com/lishangbu)

## 许可证

本项目基于 [AGPL-3.0](./LICENSE) 许可证发布。
