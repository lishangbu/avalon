---
applyTo: "*Test.java"
---

# 单元测试规范

## 基本要求

- 测试类以 Test 结尾，放在 test 对应包
- 使用 JUnit 5 + Mockito，覆盖率目标 80%+
- AAA 结构，每个测试只验证一个点
- 类与方法必须有 JEP 467 Markdown 注释（`///`），说明场景、输入、预期
- 静态导入断言与 Mockito 验证

## 命名

- 类名：被测类名 + Test
- 方法：should + 预期 + When + 条件；简单场景可用 test + 方法名
- Mock：mock + 类名

## 结构与数据

- 依赖用 Mockito/Stub，测试可重复
- 测试数据用 Builder/Fixture
- 使用 @BeforeEach 隔离状态
- 需要容器时复用模块基类配置

## 断言与覆盖

- 断言优先 AssertJ；异常用 assertThrows
- 验证交互次数与参数，并有结果断言
- 覆盖正常、边界、异常；包含空集合/空结果/错误码
- 参数校验需专用用例

## 维护

- 需求变更同步更新测试与注释
- 统一格式化与检查（spotless/maven）
- 禁止无关示例与冗余代码
