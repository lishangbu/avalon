---
applyTo: "**/*Mapstruct.java"
---

# MapStruct 规范

- Mapper 放 mapstruct 包
- @Mapper(componentModel = SPRING)
- 命名：XxxMapper
- 有 JavaDoc

## 方法

- to{目标}
- resolve/convert/extract + 描述

## 注解

- @Mapping 字段不同
- expression 或 @Named 复杂转换
- 点号嵌套属性

## 类型冲突

- 全限定名

## 依赖

- abstract class + protected 字段注入
- @Named 自定义方法

## 测试

- 复杂转换写单测

## 维护

- 同步更新映射
