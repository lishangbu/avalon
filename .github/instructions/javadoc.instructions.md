---
applyTo: "*.java"
---

# JavaDoc 规范

## 基本要求

- 公共类/接口/方法/字段必须写 JavaDoc
- 面向业务语义，说明职责、输入输出与异常
- 简体中文，结尾不使用中文句号
- 可用 {@code}/{@link} 增强可读性

## 类与接口

- 说明用途、场景、约束与扩展点
- 实体类写业务含义与关系字段
- 工具类说明可复用范围与线程/状态要求

## 方法

- 描述行为与副作用
- @param/@return/@throws 必写且含义清晰
- 强调对外契约，避免实现细节

## 字段

- 说明角色、约束、取值范围与关系
- 有默认值需说明业务意义

## 标签

- 必要：@param @return @throws
- 可选：@see @since @author @version @deprecated @apiNote @implNote @implSpec
- 避免重复信息

## 质量与维护

- 变更同步更新注释
- 保持注释与代码一致

## JEP 467

- 支持模块使用 `///` Markdown 注释
- 标签仍需保留
- 特殊字符用 {@code} 或转义
- 定期验证生成结果

