# Avalon

[![CI](https://github.com/lishangbu/avalon/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/lishangbu/avalon/actions/workflows/ci.yml)
[![Deploy Snapshot](https://github.com/lishangbu/avalon/actions/workflows/deploy-snapshot.yml/badge.svg)](https://github.com/lishangbu/avalon/actions/workflows/deploy-snapshot.yml)
[![Deploy Release](https://github.com/lishangbu/avalon/actions/workflows/deploy-release.yml/badge.svg)](https://github.com/lishangbu/avalon/actions/workflows/deploy-release.yml)
[![Publish DockerHub](https://github.com/lishangbu/avalon/actions/workflows/publish-dockerhub.yml/badge.svg)](https://github.com/lishangbu/avalon/actions/workflows/publish-dockerhub.yml)
[![License](https://img.shields.io/github/license/lishangbu/avalon)](https://github.com/lishangbu/avalon/blob/main/LICENSE)
[![Gradle](https://img.shields.io/badge/Gradle-9.4.1-02303A?logo=gradle&logoColor=white)](https://gradle.org/)
[![Java](https://img.shields.io/badge/Java-25-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.20-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Quarkus](https://img.shields.io/badge/Quarkus-3.34.5-4695EB?logo=quarkus&logoColor=white)](https://quarkus.io/)

[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Valkey](https://img.shields.io/badge/Valkey-9.0.3-FF4438?logo=redis&logoColor=white)](https://valkey.io/)
[![Flyway](https://img.shields.io/badge/Flyway-12.0.0-CC0200?logo=flyway&logoColor=white)](https://flywaydb.org/)
[![OpenAPI](https://img.shields.io/badge/OpenAPI-SmallRye-6BA539?logo=swagger&logoColor=white)](https://swagger.io/specification/)

## 内容目录

- [项目概览](#项目概览)
- [本地开发](#本地开发)
- [维护者](#维护者)
- [贡献](#贡献)
- [许可证](#许可证)

## 项目概览

**Avalon**是一个类似于宝可梦的游戏， 是一次采用纯自然语言编程的尝试。
基于DDD架构，以Quarkus+Vert.x响应式技术栈为核心，力求在性能、可维护性和长期演进之间找到平衡。

## 本地开发

### 前置条件

- JDK `25`
- Docker Desktop

`%dev` 和 `%test` profile 默认使用 Quarkus Dev Services 自动拉起 PostgreSQL 与 Valkey，因此本地运行测试或 `quarkusDev`
前需要先启动 Docker Desktop。

### 常用命令

Unix-like shell:

```bash
./gradlew test
./gradlew :apps:avalon-app:quarkusDev
```

Windows PowerShell:

```powershell
.\gradlew.bat test
.\gradlew.bat :apps:avalon-app:quarkusDev
```

### Docker Compose 运行

仓库根目录提供了一个可直接运行的 [compose.yaml](/C:/Users/baoy/Documents/study/avalon/compose.yaml:1)，默认会拉起：

- `docker.io/slf4j/avalon-admin-ui:latest`
- `docker.io/slf4j/avalon:latest`
- `postgres:18.3`
- `valkey/valkey:9.0.3`
- `prom/prometheus:latest`

直接启动：

```bash
docker compose up -d
```

首次启动会自动执行 Flyway migration，应用启动后可通过 `http://localhost:8080/q/health` 检查服务状态。
前端默认会运行在 `http://localhost:3000`，并将 `/api/*` 代理到后端服务。
Prometheus 默认运行在 `http://localhost:9090`，并抓取后端的 `http://app:8080/q/metrics`。
Compose 会使用后端自身的 `/q/health/ready` 作为 `app` 健康检查，前端和 Prometheus 会在后端进入 ready 后再启动。

如需覆盖镜像或端口，可在启动前设置环境变量：

```bash
AVALON_FRONTEND_IMAGE=docker.io/slf4j/avalon-admin-ui:latest
AVALON_IMAGE=docker.io/slf4j/avalon:latest
AVALON_FRONTEND_PORT=3000
AVALON_HTTP_PORT=8080
AVALON_DB_PORT=5432
AVALON_VALKEY_PORT=6379
AVALON_PROMETHEUS_PORT=9090
docker compose up -d
```

停止并清理：

```bash
docker compose down -v
```

### 调试入口

- 健康探针：`/q/health`
- Prometheus 指标：`/q/metrics`
- Prometheus UI：`http://localhost:9090`
- OpenAPI：`/q/openapi`
- Swagger UI：`/q/swagger-ui`

## 维护者

[ShangBu Li](https://github.com/lishangbu)

## 贡献

欢迎直接提交 Issue 或 PR，一起推进 Avalon 的后端演进。

## 许可证

[AGPL-3.0](https://opensource.org/license/agpl-v3)

Copyright (c) 2024-present, ShangBu Li
