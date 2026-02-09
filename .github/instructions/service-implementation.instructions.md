---
applyTo: "*ServiceImpl.java"
---

# ServiceImpl 规范

- 类名以 ServiceImpl 结尾，实现对应 Service
- 使用 @Service，构造器注入
- 方法注释说明用途、参数、返回值、异常
- 复杂逻辑拆私有方法

## 命名

- listXXX / getXXXPage / getXXXById
- saveXXX / createXXX
- updateXXX / updateXXXById
- removeXXX / removeXXXById
- batchXXX

## 事务与异常

- 变更方法用 @Transactional
- 抛出业务异常

## 参数校验

- 用 Bean Validation
- 优先 DTO

## 性能

- 批量处理避免大事务
- 幂等落实

## 日志

- 记录关键步骤与异常

## 维护

- 同步更新注释
- 遵循项目风格
