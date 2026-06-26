---
name: spring-boot-4
description: "Use when designing, implementing, or reviewing Spring Boot 4 backend code in avalon: app startup, modules, starters, controllers, configuration properties, CORS, JSON, error responses, tests, and runtime assembly."
---

# Spring Boot 4 后端约束

## 使用范围

用于 `avalon` 后端的 Spring Boot 4 结构、依赖、配置、Web API 和测试。

## 模块规则

- 只有 `app` 应用 `org.springframework.boot` 插件并提供可运行入口。
- 业务模块默认是 library 模块，按需使用 `kotlin("plugin.spring")`。
- `app` 负责装配 `security`、`system`、`migration` 和 `common-persistence`。
- 不要重新引入目录、数据导入或战斗模块装配。

## Web 和配置

- API 路径统一以 `/api` 开头。
- Controller 只做协议适配、参数校验和响应转换；业务规则放 Service。
- 配置使用 `@ConfigurationProperties`，不要散落 `@Value`。
- CORS 配置只放在应用装配边界，默认允许管理端本地开发地址。
- DTO 是 JSON 边界，不直接暴露 Jimmer 实体或内部异常对象。

## 错误响应

- 使用稳定错误模型或 Spring 标准错误能力。
- 错误响应不暴露 SQL、栈信息、内部类名和敏感参数。
- 用户可见错误文案使用清晰中文；机器字段名保持稳定英文。

## 测试

- Web 层使用 MockMvc 或 Spring 测试切片。
- 启动装配使用 `app` 的上下文测试。
- 修改配置或自动装配时，至少跑对应模块测试和 `:app:test`。
