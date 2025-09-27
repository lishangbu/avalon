---
applyTo: "**"
---

# JavaDoc 编写规范

## 基本要求

- 所有公共类、接口、方法、字段必须编写 JavaDoc 注释
- 注释内容应简明扼要，突出用途、参数、返回值和异常
- 注释统一使用简体中文，表达清晰、易于理解
- 避免无意义的注释，如"getter 方法"或"设置 xxx"
- 注释应体现业务语义，便于开发者快速理解代码意图
- 注释结尾不使用中文句号，保持简洁
- 使用 JDK 17 的多行文本块格式提升可读性

## 类和接口

- 描述类/接口的主要功能和应用场景
- 必要时补充设计思路或注意事项
- 对于实体类，说明其业务含义和主要用途
- 对于服务类，说明提供的核心业务功能
- 对于工具类，说明适用场景和使用方式

### 示例

```java
/**
 * 用户实体类，封装用户的基本信息和认证状态
 * <p>
 * 包含用户的身份信息、权限状态、创建和更新时间等核心数据
 * 支持软删除机制，通过 deleted 字段标识删除状态
 */
@Entity
public class User {
    // ...
}

/**
 * 用户业务服务接口，定义用户相关的核心业务操作
 * <p>
 * 提供用户注册、认证、信息管理等功能
 * 所有操作均需考虑权限验证和数据安全
 */
public interface UserService {
    // ...
}

/**
 * OAuth2 令牌工具类，提供 JWT 令牌的生成、解析和验证功能
 * <p>
 * 基于 RSA 算法进行签名，支持自定义过期时间和用户信息载荷
 * 线程安全，可在多线程环境下使用
 */
@Component
public class TokenUtils {
    // ...
}
```

## 方法

- 描述方法的功能和业务含义
- 使用 `@param` 说明每个参数的含义和约束条件
- 使用 `@return` 说明返回值的含义和数据结构
- 使用 `@throws` 说明可能抛出的异常及其触发条件
- 对于复杂的业务逻辑，补充关键步骤说明
- 对于有副作用的操作，明确说明影响范围

### 示例

```java
/**
 * 根据用户名和密码进行用户认证
 * <p>
 * 验证用户凭据有效性，成功后生成 JWT 令牌
 * 登录失败会记录审计日志，连续失败会触发账户锁定机制
 *
 * @param username 用户名，不能为空且长度不超过50个字符
 * @param password 密码，不能为空且必须符合密码强度要求
 * @return 认证成功返回包含用户信息和令牌的认证结果
 * @throws AuthenticationException 用户名或密码错误时抛出
 * @throws AccountLockedException 账户被锁定时抛出
 * @throws IllegalArgumentException 参数为空或不符合格式要求时抛出
 */
AuthenticationResult authenticate(String username, String password);

/**
 * 分页查询符合条件的用户列表
 * <p>
 * 支持按用户名、邮箱、状态等条件进行模糊查询
 * 查询结果按创建时间倒序排列，只返回未删除的用户
 *
 * @param queryParam 查询条件参数，允许为空表示查询所有用户
 * @param pageable 分页参数，包含页码、每页大小和排序规则
 * @return 用户分页数据，包含用户列表和分页信息
 */
Page<UserVO> getUsers(UserQueryParam queryParam, Pageable pageable);

/**
 * 批量更新用户状态
 * <p>
 * 原子性操作，要么全部成功要么全部失败
 * 状态变更会触发相关的业务事件通知
 *
 * @param userIds 用户ID列表，不能为空且不能包含重复ID
 * @param status 目标状态，必须是有效的用户状态枚举值
 * @return 实际更新的用户数量
 * @throws IllegalArgumentException 参数无效时抛出
 * @throws DataAccessException 数据库操作失败时抛出
 */
@Transactional
int batchUpdateStatus(List<Long> userIds, UserStatus status);
```

## 字段

- 说明字段的业务含义和数据特征
- 对于枚举字段，说明可能的取值
- 对于关联字段，说明关联关系
- 对于有特殊约束的字段，说明约束条件

### 示例

```java
/**
 * 用户唯一标识符，数据库主键
 */
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

/**
 * 用户名，用于登录认证，全局唯一
 * 长度限制：3-50个字符，支持字母、数字、下划线
 */
@Column(unique = true, nullable = false, length = 50)
private String username;

/**
 * 用户状态枚举，标识账户的可用性
 * 可选值：ACTIVE(活跃)、INACTIVE(未激活)、LOCKED(锁定)、DISABLED(禁用)
 */
@Enumerated(EnumType.STRING)
private UserStatus status;

/**
 * 软删除标识，true表示已删除，false表示正常状态
 * 删除的用户不会物理删除，仅标记为已删除状态
 */
@Column(nullable = false)
private Boolean deleted = false;

/**
 * 用户角色集合，一个用户可以拥有多个角色
 * 使用延迟加载策略，按需查询角色信息
 */
@ManyToMany(fetch = FetchType.LAZY)
@JoinTable(name = "user_role")
private Set<Role> roles = new HashSet<>();
```

## 常用标签

### 核心标签
- `@param` - 参数说明
- `@return` - 返回值说明  
- `@throws` / `@exception` - 异常说明
- `@see` - 参考链接或相关类/方法
- `@since` - 版本信息
- `@author` - 作者信息（可选）
- `@version` - 版本号（可选）

### 补充标签
- `@deprecated` - 标记过时的API，说明替代方案
- `@apiNote` - API使用说明和注意事项
- `@implNote` - 实现细节说明
- `@implSpec` - 实现规范说明

### 示例

```java
/**
 * 用户密码加密工具类
 * <p>
 * 基于 BCrypt 算法提供密码加密和验证功能
 * 默认使用 12 轮加密强度，兼顾安全性和性能
 *
 * @author avalon-team  
 * @version 1.0.0
 * @since 1.0.0
 * @see org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
 */
public class PasswordEncoder {

    /**
     * 对明文密码进行加密
     *
     * @param rawPassword 明文密码，不能为空
     * @return 加密后的密码哈希值
     * @throws IllegalArgumentException 密码为空时抛出
     * @apiNote 每次加密同一密码会产生不同的哈希值，这是正常现象
     * @implNote 使用 BCrypt 算法，加密强度为12轮
     */
    public String encode(String rawPassword) {
        // ...
    }

    /**
     * 验证明文密码与加密密码是否匹配
     *
     * @deprecated 建议使用 {@link #matches(String, String)} 方法
     * @param rawPassword 明文密码
     * @param encodedPassword 加密后的密码
     * @return 密码匹配返回true，否则返回false
     */
    @Deprecated(since = "1.1.0", forRemoval = true)
    public boolean verify(String rawPassword, String encodedPassword) {
        return matches(rawPassword, encodedPassword);
    }
}
```

## 格式规范

### 基本格式
```java
/**
 * 单行描述
 * <p>
 * 详细描述（可选）
 * 可以分多个段落说明
 *
 * @param paramName 参数说明
 * @return 返回值说明
 * @throws ExceptionType 异常触发条件
 * @see 相关引用
 * @since 版本信息
 */
```

### 代码示例
在注释中包含代码示例时使用 `{@code}` 或代码块：

```java
/**
 * 解析 JWT 令牌获取用户信息
 * <p>
 * 使用示例：
 * <pre>{@code
 * String token = "eyJhbGciOiJSUzI1NiJ9...";
 * UserInfo userInfo = tokenUtils.parseToken(token);
 * System.out.println("用户ID: " + userInfo.getUserId());
 * }</pre>
 *
 * @param token JWT 令牌字符串
 * @return 解析出的用户信息对象
 * @throws TokenExpiredException 令牌已过期
 * @throws TokenInvalidException 令牌格式无效或签名验证失败
 */
public UserInfo parseToken(String token) {
    // ...
}
```

## 质量要求

### 必须避免
- 重复代码中已有的信息
- 无意义的描述（如"这是一个getter方法"）
- 过时或错误的注释信息
- 中英文混用

### 应该包含
- 业务含义和使用场景
- 重要的约束条件和注意事项
- 参数的有效值范围或格式要求
- 可能的副作用或影响范围
- 相关的业务规则说明

### 维护要求
- 代码变更时同步更新注释
- 定期检查注释的准确性
- 及时标记过时的API并提供替代方案

## 其他建议

- 注释内容随代码变更及时维护
- 推荐参考 Spring 官方文档和项目已有注释风格
- 使用 IDE 的 JavaDoc 预览功能验证格式正确性
- 对于公共API，注释应更加详细和规范
- 考虑使用 `{@link}` 标签引用相关的类或方法
- 复杂的算法或业务逻辑建议添加实现思路说明
