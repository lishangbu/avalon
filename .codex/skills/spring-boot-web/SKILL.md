---
name: spring-boot-web
description: "Spring Boot 4 and Spring Web development in Avalon. Use for application startup, controllers, MVC endpoints, validation, configuration properties, CORS, error responses, auto-configuration, starters, dependency injection, Boot tests, or runtime assembly."
---

# Spring Boot 与 Web

## 修改前

- 从 version catalog 确认 Spring Boot 版本，并检查目标代码属于应用装配、Web 协议还是可复用自动配置。
- 涉及 JSON、鉴权、OpenAPI 或持久化时，同时加载对应技术栈 skill。
- 领域 API 变更先读 `CONTEXT.md` 与相关 ADR。

## 项目要求

- 仅 `app` 提供可运行入口；其他模块通过配置类、组件扫描或 starter 暴露能力。
- Controller 只做协议适配、输入校验和响应转换；业务规则与事务放服务层。
- API 路径以 `/api` 开头；管理接口使用稳定资源路径，不把内部类名暴露成协议。
- 使用 `@ConfigurationProperties` 表达配置；不要散落 `@Value` 或静态读取环境变量。
- CORS、全局 JSON 与异常处理放应用边界；错误响应不泄露 SQL、栈、token、secret 或内部类名。
- REST 响应使用 DTO，不直接返回 Jimmer 实体或框架异常对象。
- 自动配置使用明确条件，禁用功能时不得创建依赖缺失的 bean。
- 公共配置、扩展点与 starter 接入契约使用中文 KDoc。

## 测试驱动

- Controller 行为先写 MockMvc/测试切片失败用例。
- 装配、条件 bean 和配置绑定先写 ApplicationContext 测试。
- 错误路径覆盖校验失败、无权限、资源不存在和稳定错误结构。

## 完成标准

- 目标 Web 切片或上下文测试通过。
- 配置变更同时覆盖默认值、显式值和禁用分支。
- API 变更同步验证 Springdoc 契约；安全端点同步验证后端授权。
- 应用装配或自动配置变更额外运行 `:app:test` 和拥有模块测试。
