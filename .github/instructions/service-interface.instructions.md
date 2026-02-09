---
applyTo: "*Service.java"
excludeApplyTo: "*ServiceImpl.java"
---

# Service 接口规范

## 基本要求

- 类名以 Service 结尾，放在 service 包，仅定义契约
- 方法签名体现业务语义，避免频繁变更
- 必须有 JEP 467 Markdown 注释（`///`），说明用途、参数、返回值、异常
- 参数/返回优先明确类型或 DTO，避免 Object
- 复杂输入用请求对象封装

## 命名

- listXXX / getXXXPage / getXXXById
- updateXXXById / saveXXX / removeXXXById
- batchXXX

## 契约与异常

- 明确前置/后置/边界行为
- 用 @throws 写清异常触发条件与处理建议
- 返回值说明空集合/空对象语义，避免 null

## 设计与维护

- 单一职责，不暴露实现细节
- 版本演进优先新增方法
- 变更同步更新注释与格式化要求
