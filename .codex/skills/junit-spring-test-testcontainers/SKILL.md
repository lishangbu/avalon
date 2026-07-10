---
name: junit-spring-test-testcontainers
description: "Backend testing in Avalon with Kotlin test, JUnit 5, Spring Test, MockMvc, and Testcontainers. Use when adding or changing tests, fixtures, test slices, integration tests, PostgreSQL containers, assertions, test performance, deterministic clocks, or when applying TDD to backend behavior."
---

# JUnit、Spring Test 与 Testcontainers

## 测试驱动循环

1. 先写最小失败测试，运行并确认它因目标行为缺失而失败。
2. 实现最少代码使测试通过。
3. 在全绿状态重构，并补充必要边界。
4. 运行受影响技术栈规定的集成验证。

## 项目要求

- 测试名称描述行为与边界，不复述方法名。
- 纯领域逻辑使用快速 Kotlin/JUnit 测试；Web 使用 MockMvc 或切片；装配使用 Spring context；真实持久化使用 PostgreSQL Testcontainers。
- 不用 H2 替代 PostgreSQL 专属 schema、SQL 或事务语义。
- fixture 只提供当前测试需要的数据；稳定 ID、时钟和随机源必须可控。
- 不使用任意 sleep；异步行为使用可观察状态、虚拟时钟或有界等待。
- 不 mock 被测对象内部实现；在外部边界使用替身，并断言用户可观察结果。
- 测试不得依赖执行顺序、共享可变数据库或本机已有服务。
- 失败场景同时断言错误类型、稳定响应和事务回滚。

## 风险分层

- 单一算法：聚焦测试。
- Controller/JSON：聚焦测试 + Web/序列化测试。
- Jimmer/Liquibase/Security：Testcontainers 集成测试。
- Gradle、装配、跨技术栈或提交前：受影响模块测试，必要时完整 `test`。

## 完成标准

- 红灯证据与绿灯命令可说明。
- 新测试在单独运行和测试套件中都稳定。
- 没有跳过、隔离或放宽断言来掩盖缺陷。
- Docker 或外部条件不可用时明确报告未验证项，不把跳过当作通过。
