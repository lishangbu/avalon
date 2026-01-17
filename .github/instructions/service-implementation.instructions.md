---
applyTo: "*ServiceImpl.java"
---

# ServiceImpl 代码与注释编写规范

## 基本要求

- 类名以 *ServiceImpl 结尾，位于 service.impl 包，实现对应 Service 接口
- 使用 @Service（或 @Component）标记并采用构造器注入（Lombok @RequiredArgsConstructor 优先）
- 方法注释需说明业务含义、参数、返回值与异常，遵循 JEP 467 Markdown 文档注释
- 复杂逻辑拆分为私有方法，保持单一职责

## 命名规约

- 查询列表：listXXX
- 获取分页：getXXXPage / pageXXX
- 根据ID查询：getXXXById
- 保存：saveXXX / createXXX
- 更新：updateXXX / updateXXXById
- 删除：removeXXX / removeXXXById
- 批量：batchXXX

## 事务与异常

- 涉及数据变更的方法必须使用 @Transactional；查询默认不启用事务
- 明确事务传播与隔离级别需求，读写分离场景遵循项目统一约定
- 抛出具备业务语义的异常，禁止吞异常或返回 null 掩盖错误

## 参数与校验

- 入口参数需使用 Bean Validation 描述约束，业务层补充必要的显式校验
- 对外暴露的输入对象优先使用 DTO，避免直接暴露实体
- 对外部依赖结果进行非空与状态校验，避免脏数据继续流转

## 性能与资源

- 批量操作采用批处理或分段处理，避免大事务与长时间锁
- 谨慎使用分布式锁或缓存更新，保持一致性并记录关键日志
- 需要幂等的接口，应在 Service 层落实幂等策略与检测

## 日志与审计

- 记录关键业务步骤与异常栈，避免在正常路径中打印敏感数据
- 审计字段的维护（如创建人、更新时间）应统一封装，避免散落在业务代码

## 文档与维护

- 变更方法时同步更新注释与接口契约
- 遵循项目编码风格和格式化工具（spotless/maven）
- 禁止在 Service 层直接处理 HTTP/Controller 逻辑
