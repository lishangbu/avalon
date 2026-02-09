---
applyTo: "**"
---

# 项目开发规范

## 技术栈.trellis/

- 框架: Spring Boot 4.1.0-M1
- ORM框架: Spring Data Jpa
- 代码校验与格式化: spotless
- 开发语言: Java
- 项目管理工具: maven(maven-wrapper)
- APT辅助工具: lombok
- 日志: slf4j + logback
- 安全: spring-oauth2-authorization-server
- 数据库连接池: HikariCP
- 缓存: caffeine
- 文档: springdoc-openapi + swagger-ui
- 单元测试: JUnit + Mockito
- 持续集成: GitHub Actions

## 代码

## 代码规范

- 代码整洁、可读，提取共用逻辑
- 注释面向开发者
- 代码可直接运行，补齐必要导入与依赖
- 遵循项目既有风格

## 其他

- 优先使用现有依赖
