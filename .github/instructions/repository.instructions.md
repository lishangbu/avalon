---
applyTo: "*Repository.java"
---

# Repository 规范

- 类名以 Repository 结尾
- 继承 JpaRepository
- 仅持久化，无业务逻辑
- 接口方法有 JavaDoc

## 命名

- findBy/getBy
- findAllBy
- existsBy
- countBy
- deleteBy/removeBy
- findAllByIdIn/deleteAllByIdIn
- findAllByXXXOrderByYYY(Desc)
- findFirstBy/findTopNBy

## 自定义查询

- @Query + @Param
- 修改加 @Modifying
- 原生 SQL nativeQuery = true

## 分页

- Pageable -> Page/Slice

## 事务

- 查询不加事务
- 修改事务在 Service

## 其他

- 避免业务逻辑
- 复杂条件用 Specification
- 注意索引与批量
