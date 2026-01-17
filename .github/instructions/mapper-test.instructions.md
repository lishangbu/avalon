---
applyTo: "*MapperTest.java"
---

# Mapper 层测试编写规范

## 基本要求

- 所有 Mapper 测试类必须继承 `AbstractMapperTest`
- 使用 `@MybatisPlusTest` 启用 MyBatis-Plus 测试支持
- 测试方法命名遵循 `should + 预期结果 + When + 测试条件`
- 采用 AAA（Arrange-Act-Assert）模式组织代码
- 测试类与方法需编写 JavaDoc/Markdown 文档注释

## 容器复用

- 禁止在测试类中直接声明 `@Container` PostgreSQL 容器
- 通过继承 `AbstractMapperTest` 复用共享容器，测试开始前启动、结束后关闭
- 测试数据与表结构依赖 Liquibase 迁移，确保在测试执行前完成

## 抽象基类与配置要求

- 每个模块测试根包下提供 `AbstractMapperTest`，集中管理容器与测试上下文
- 测试环境配置类应显式配置 Liquibase 和 MyBatis-Plus，避免依赖外部 `application.yml`
- 容器启动顺序：启动 PostgreSQL 容器 → `@ServiceConnection` 注入数据源 → Liquibase 同步迁移 → MyBatis-Plus 初始化

## 编码与校验

- 参数与实体校验使用 Bean Validation 注解，保证入参合法性
- 测试方法相互独立，不依赖执行顺序或外部状态
- 每个测试仅覆盖单一功能点，断言表达预期业务结果
- 断言可使用静态导入简化表达式

## 其他约定

- 路径与命名遵循模块包结构，保持文件布局一致
- 异常处理与日志记录依赖全局配置，无需在测试中重复实现
- 变更测试用例时同步更新注释与迁移脚本，保证可维护性
