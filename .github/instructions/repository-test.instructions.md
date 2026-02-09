---
applyTo: "*RepositoryTest.java"
---

# Repository 测试规范

- 继承 AbstractRepositoryTest
- @DataJpaTest
- 命名：should + 预期 + When + 条件
- AAA 结构

## 容器

- 禁止声明 @Container
- 通过 AbstractRepositoryTest 复用容器

## 编码

- Bean Validation 入参
- 测试独立、可重复
- 单测一个功能点

## 其他

- 遵循包结构
- 同步更新注释
