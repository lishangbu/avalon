# Avalon

[![CI](https://github.com/lishangbu/avalon/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/lishangbu/avalon/actions/workflows/ci.yml)
[![Deploy Snapshot](https://github.com/lishangbu/avalon/actions/workflows/deploy-snapshot.yml/badge.svg)](https://github.com/lishangbu/avalon/actions/workflows/deploy-snapshot.yml)
[![Deploy Release](https://github.com/lishangbu/avalon/actions/workflows/deploy-release.yml/badge.svg)](https://github.com/lishangbu/avalon/actions/workflows/deploy-release.yml)
[![Publish DockerHub](https://github.com/lishangbu/avalon/actions/workflows/publish-dockerhub.yml/badge.svg)](https://github.com/lishangbu/avalon/actions/workflows/publish-dockerhub.yml)
[![License](https://img.shields.io/github/license/lishangbu/avalon)](https://github.com/lishangbu/avalon/blob/main/LICENSE)
[![Gradle](https://img.shields.io/badge/Gradle-9.4.1-02303A?logo=gradle&logoColor=white)](https://gradle.org/)
[![Java](https://img.shields.io/badge/Java-25-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.20-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Quarkus](https://img.shields.io/badge/Quarkus-3.34.3-4695EB?logo=quarkus&logoColor=white)](https://quarkus.io/)

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

### 调试入口

- 应用探针：`/api/app-info`
- OpenAPI：`/q/openapi`
- Swagger UI：`/q/swagger-ui`

## 维护者

[ShangBu Li](https://github.com/lishangbu)

## 贡献

欢迎直接提交 Issue 或 PR，一起推进 Avalon 的后端演进。

## 许可证

[AGPL-3.0](https://opensource.org/license/agpl-v3)

Copyright (c) 2024-present, ShangBu Li
