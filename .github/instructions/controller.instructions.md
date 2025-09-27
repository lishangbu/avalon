---
applyTo: "*Controller.java"
---

# Controller 代码与注释编写规范

## Controller 规范

- 命名以 *Controller 结尾，放在 controller 包
- 负责接收请求、参数校验、调用 Service 层，不包含业务逻辑
- 使用 @RestController 注解标记
- 控制器和方法必须编写 JavaDoc 注释，说明接口用途、参数、返回值
- 统一使用 RESTful 风格的 URL 设计
- 参数校验使用 Bean Validation 注解
- 异常处理统一使用全局异常处理器
- 注释结尾不使用中文句号，保持简洁

## 命名规约

- 查询列表：`listXXX` 或 `getXXXs`
- 获取分页：`getXXXPage` 或 `pageXXX`
- 根据ID查询：`getXXXById`
- 新增：`createXXX` 或 `addXXX`
- 更新：`updateXXX` 或 `updateXXXById`
- 删除：`deleteXXXById` 或 `removeXXXById`
- 批量操作：`batchXXX`

## HTTP 方法映射

- `GET` - 查询操作
- `POST` - 新增操作
- `PUT` - 更新操作（全量更新）
- `PATCH` - 更新操作（部分更新）
- `DELETE` - 删除操作

## 示例

```java
/**
 * 用户管理控制器，提供用户相关的 REST API 接口
 * <p>
 * 包含用户的查询、新增、更新、删除等基本操作
 * 支持分页查询、条件查询和批量操作
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;

    /**
     * 分页查询用户列表
     * <p>
     * 支持按用户名、邮箱、状态等条件进行筛选查询
     * 结果按创建时间倒序排列
     *
     * @param queryParam 查询条件参数
     * @param pageable 分页参数，包含页码、页大小和排序规则
     * @return 用户分页数据，包含用户列表和分页信息
     */
    @GetMapping
    public Page<UserVO> getUserPage(@Valid UserQueryParam queryParam, Pageable pageable) {
        return userService.getUsers(queryParam, pageable);
    }

    /**
     * 根据用户ID查询用户详细信息
     *
     * @param id 用户ID，必须为正整数
     * @return 用户详细信息
     */
    @GetMapping("/{id:\\d+}")
    public UserVO getUserById(@PathVariable @Min(1) Long id) {
        return userService.getUserById(id);
    }

    /**
     * 创建新用户
     * <p>
     * 用户名和邮箱必须全局唯一，密码需符合强度要求
     * 创建成功后会发送激活邮件到用户邮箱
     *
     * @param createRequest 用户创建请求参数
     * @return 创建成功的用户信息
     */
    @PostMapping
    public User createUser(@Valid @RequestBody UserCreateRequest createRequest) {
        return userService.createUser(createRequest);
    }

    /**
     * 更新用户信息
     * <p>
     * 支持部分字段更新，空值字段不会被更新
     * 敏感信息（如密码）需要通过专门接口修改
     *
     * @param updateRequest 用户更新请求参数
     * @return 更新后的用户信息
     */
    @PutMapping
    public User updateUser(@Valid @RequestBody UserUpdateRequest updateRequest) {
        return userService.updateUserById(updateRequest);
    }

    /**
     * 删除用户（软删除）
     * <p>
     * 执行软删除操作，用户数据不会物理删除
     * 删除后用户无法登录，相关数据保留用于审计
     *
     * @param id 用户ID，必须为正整数
     */
    @DeleteMapping("/{id:\\d+}")
    public void removeUser(@PathVariable @Min(1) Long id) {
        userService.removeUserById(id);
    }

    /**
     * 批量更新用户状态
     * <p>
     * 原子性操作，要么全部成功要么全部失败
     * 状态变更会触发相关的业务事件通知
     *
     * @param request 批量状态更新请求，包含用户ID列表和目标状态
     * @return 实际更新的用户数量
     * @throws IllegalArgumentException 请求参数无效时抛出
     */
    @PatchMapping("/status")
    public Integer batchUpdateStatus(@Valid @RequestBody BatchStatusUpdateRequest request) {
        return userService.batchUpdateStatus(request.getUserIds(), request.getStatus());
    }

    /**
     * 根据用户名查询用户
     * <p>
     * 用于用户名唯一性检查等场景
     *
     * @param username 用户名，不能为空
     * @return 用户信息，如果不存在则抛出异常
     */
    @GetMapping("/valid-username/{username}")
    public UserVO getUserByUsername(@PathVariable @NotBlank String username) {
        return userService.getUserByUsername(username);
    }

    /**
     * 获取当前登录用户信息
     *
     * @param authentication 当前认证信息，由 Spring Security 注入
     * @return 当前登录用户的详细信息
     */
    @GetMapping("/me")
    public UserVO getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        return userService.getUserByUsername(username);
    }
}
```

## 参数校验规范

使用 Bean Validation 注解进行参数校验：

```java
/**
 * 用户创建请求参数
 */
public class UserCreateRequest {

    /**
     * 用户名，长度3-50个字符，支持字母、数字、下划线
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    private String username;

    /**
     * 邮箱地址，必须符合邮箱格式
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 密码，长度8-20个字符，必须包含字母和数字
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 20, message = "密码长度必须在8-20个字符之间")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*?&]{8,}$",
            message = "密码必须包含至少一个字母和一个数字")
    private String password;

    // getter/setter...
}
```

## 其他规范

- **路径参数校验**：使用 `@PathVariable` 配合校验注解
- **查询参数校验**：使用 `@RequestParam` 配合校验注解
- **请求体校验**：使用 `@RequestBody @Valid` 进行校验
- **响应状态码**：遵循 HTTP 状态码规范
- **API 版本控制**：通过 Spring 7 RequestMapping新增的version参数进行版本管理
- **跨域处理**：使用 `@CrossOrigin` 注解或全局配置
- **接口文档**：使用 OpenAPI 3.0 注解生成接口文档
- **日志记录**：关键操作记录操作日志，便于问题排查
- **统一响应处理**：项目使用统一拦截器处理响应格式，Controller 层直接返回业务数据
- **统一异常处理**：项目使用全局异常处理器统一处理异常
