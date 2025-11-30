---
applyTo: "*Repository.java"
---

# Repository 代码与注释编写规范

## Repository 规范

- 命名以 *Repository 结尾，放在 repository 包
- 继承 ListCrudRepository 或 PagingAndSortingRepository
- 负责数据持久化操作，不包含业务逻辑
- 接口和方法必须编写 JavaDoc 注释，说明查询/操作目的、参数、返回值
- 使用 @Repository 注解标记（可选，Spring Data JDBC 自动注册）
- 注释结尾不使用中文句号，保持简洁

### 命名规约

- 优先按 Spring Data JDBC 规范命名查询方法
- 单条查询：`findByXXX`、`getByXXX`
- 列表查询：`findAllByXXX`
- 存在性检查：`existsByXXX`
- 计数查询：`countByXXX`
- 删除操作：`deleteByXXX`、`removeByXXX`
- 批量操作：`findAllByIdIn`、`deleteAllByIdIn`
- 排序查询：`findAllByXXXOrderByYYY`、`findAllByXXXOrderByYYYDesc`
- 限制结果：`findFirstByXXX`、`findTopNByXXX`

## 自定义查询规范

- 复杂查询使用 @Query 注解
- 参数使用 @Param 注解明确指定
- 修改操作使用 @Modifying 注解
- 自定义查询方法也必须编写详细的 JavaDoc 注释

## 分页查询规范

- 分页参数使用 Pageable 接口
- 返回类型使用 Page<T> 或 Slice<T>
- 排序参数包含在 Pageable 中
- 注释中说明分页参数的作用

### 分页查询示例

```java
/**
 * 分页查询所有用户
 *
 * @param pageable 分页参数，包含页码、页大小和排序信息
 * @return 用户分页数据，包含总数、当前页数据等信息
 */
Page<User> findAll(Pageable pageable);

/**
 * 流式分页查询（不计算总数）
 *
 * @param pageable 分页参数
 * @return 用户切片数据，不包含总数信息，性能更好
 */
Slice<User> findAllBy(Pageable pageable);
```

## 事务注解使用

- 查询操作通常不需要 @Transactional
- 修改操作建议在 Service 层添加 @Transactional
- Repository 层的 @Modifying 操作需要事务支持
- 只读操作可使用 @Transactional(readOnly = true)

## 其他建议

- 避免在 Repository 层编写业务逻辑
- 复杂查询条件建议使用 Specification 或 Criteria API
- 注意查询性能，适当添加数据库索引
- 批量操作注意数据量大小，避免内存溢出
- 使用 @EntityGraph 优化关联查询，避免 N+1 问题
- 对于大数据量查询，考虑使用流式处理或分批处理
- 自定义查询的 JPQL 语句要注意实体名称而非表名
- 原生 SQL 查询要注意数据库兼容性问题
