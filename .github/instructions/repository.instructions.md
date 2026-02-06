---
applyTo: "*Repository.java"
---

# Repository 代码与注释编写规范

## Repository 规范

- 命名以 *Repository 结尾，放在 repository 包
- 继承 JpaRepository 或 PagingAndSortingRepository
- 负责数据持久化操作，不包含业务逻辑
- 接口和方法必须编写 JavaDoc 注释，说明查询/操作目的、参数、返回值
- 使用 @Repository 注解标记（可选，Spring Data JPA 自动注册）
- 注释结尾不使用中文句号，保持简洁

### 命名规约

- 优先按 Spring Data JPA 规范命名查询方法
- 单条查询：`findByXXX`、`getByXXX`
- 列表查询：`findAllByXXX`
- 存在性检查：`existsByXXX`
- 计数查询：`countByXXX`
- 删除操作：`deleteByXXX`、`removeByXXX`
- 批量操作：`findAllByIdIn`、`deleteAllByIdIn`
- 排序查询：`findAllByXXXOrderByYYY`、`findAllByXXXOrderByYYYDesc`
- 限制结果：`findFirstByXXX`、`findTopNByXXX`

### 示例

```java
/**
 * 用户数据仓库，负责用户实体的持久化操作
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根据用户名查找用户
     *
     * @param username 用户名
     * @return 用户实体的 Optional 包装
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据邮箱查找用户
     *
     * @param email 邮箱地址
     * @return 用户实体的 Optional 包装
     */
    Optional<User> findByEmail(String email);

    /**
     * 检查用户名是否已存在
     *
     * @param username 用户名
     * @return 如果存在返回 true，否则返回 false
     */
    boolean existsByUsername(String username);

    /**
     * 根据状态查找所有用户
     *
     * @param status 用户状态
     * @return 符合条件的用户列表
     */
    List<User> findAllByStatus(UserStatus status);

    /**
     * 分页查询指定状态的用户
     *
     * @param status 用户状态
     * @param pageable 分页参数
     * @return 用户分页数据
     */
    Page<User> findAllByStatus(UserStatus status, Pageable pageable);

    /**
     * 统计指定状态的用户数量
     *
     * @param status 用户状态
     * @return 符合条件的用户数量
     */
    long countByStatus(UserStatus status);

    /**
     * 根据ID列表批量查询用户
     *
     * @param ids 用户ID列表
     * @return 符合条件的用户列表
     */
    List<User> findAllByIdIn(List<Long> ids);

    /**
     * 根据ID列表批量删除用户
     *
     * @param ids 用户ID列表
     */
    void deleteAllByIdIn(List<Long> ids);

    /**
     * 根据创建时间范围查询用户
     *
     * @param startTime 开始时间（包含）
     * @param endTime 结束时间（包含）
     * @return 符合时间范围的用户列表
     */
    List<User> findAllByCreateTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查找最近创建的用户
     *
     * @param pageable 分页参数
     * @return 按创建时间倒序排列的用户分页数据
     */
    Page<User> findAllByOrderByCreateTimeDesc(Pageable pageable);

    /**
     * 查找第一个指定状态的用户
     *
     * @param status 用户状态
     * @return 符合条件的第一个用户，如果不存在则返回空
     */
    Optional<User> findFirstByStatus(UserStatus status);

    /**
     * 根据用户名模糊查询，按创建时间倒序
     *
     * @param username 用户名关键字
     * @return 符合条件的用户列表，按创建时间倒序
     */
    List<User> findAllByUsernameContainingOrderByCreateTimeDesc(String username);
}
```

## 自定义查询规范

- 复杂查询使用 @Query 注解
- 参数使用 @Param 注解明确指定
- 原生SQL查询使用 nativeQuery = true
- 修改操作使用 @Modifying 注解
- 自定义查询方法也必须编写详细的 JavaDoc 注释

### 自定义查询示例

```java
/**
 * 自定义查询：根据用户名模糊查询活跃用户
 *
 * @param username 用户名关键字，支持模糊匹配
 * @param pageable 分页参数
 * @return 符合条件的活跃用户分页数据
 */
@Query("""
    SELECT u FROM User u
    WHERE u.username LIKE %:username%
      AND u.status = 'ACTIVE'
    """)
Page<User> findActiveUsersByUsernameContaining(@Param("username") String username, Pageable pageable);

/**
 * 批量更新用户状态
 *
 * @param status 新状态
 * @param ids 用户ID列表
 * @return 实际更新的记录数
 */
@Modifying
@Query("""
    UPDATE User u
    SET u.status = :status
    WHERE u.id IN :ids
    """)
int updateStatusByIds(@Param("status") UserStatus status, @Param("ids") List<Long> ids);

/**
 * 原生SQL查询：统计每个状态的用户数量
 *
 * @return 状态统计结果，每个Object[]包含[status, count]
 */
@Query(value = """
    SELECT status, COUNT(*) as count
    FROM users
    GROUP BY status
    """, nativeQuery = true)
List<Object[]> countUsersByStatus();

/**
 * 软删除用户（更新删除标记）
 *
 * @param id 用户ID
 * @param deleteTime 删除时间
 * @return 更新的记录数
 */
@Modifying
@Query("""
    UPDATE User u
    SET u.deleted = true, u.deleteTime = :deleteTime
    WHERE u.id = :id
    """)
int softDeleteById(@Param("id") Long id, @Param("deleteTime") LocalDateTime deleteTime);

/**
 * 复杂条件查询：根据多个条件查询用户
 *
 * @param username 用户名（可选）
 * @param email 邮箱（可选）
 * @param status 状态（可选）
 * @param pageable 分页参数
 * @return 符合条件的用户分页数据
 */
@Query("""
    SELECT u FROM User u
    WHERE (:username IS NULL OR u.username LIKE %:username%)
      AND (:email IS NULL OR u.email = :email)
      AND (:status IS NULL OR u.status = :status)
    """)
Page<User> findUsersByMultipleConditions(@Param("username") String username,
                                         @Param("email") String email,
                                         @Param("status") UserStatus status,
                                         Pageable pageable);
```

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
