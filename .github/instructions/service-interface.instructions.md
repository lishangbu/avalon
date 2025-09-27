---
applyTo: "*Service.java"
excludeApplyTo: "*ServiceImpl.java"
---

# Service 接口编写规范

## Service 接口规范

- 命名以 \*Service 结尾，放在 service 包
- 定义业务逻辑接口，由 ServiceImpl 实现
- 接口方法注释需说明业务含义、参数、返回值
- 不包含实现逻辑，仅定义契约

### 命名规约

- 查询列表：`listXXX`
- 获取分页：`getXXXPage`  
- 根据ID查询：`getXXXById`
- 更新：`updateXXXById`
- 保存：`saveXXX`
- 删除：`removeXXXById`
- 批量操作：`batchXXX`

### 示例

```java
/**
 * 用户业务服务接口，定义用户相关的业务操作
 */
public interface UserService {
    
    /**
     * 分页查询用户列表
     *
     * @param pageRequest 分页参数
     * @param queryParam 查询条件
     * @return 用户分页数据
     */
    Page<User> getPageUsers(Pageable pageRequest, UserQueryParam queryParam);
    
    /**
     * 根据用户ID获取用户信息
     *
     * @param id 用户ID
     * @return 用户信息
     * @throws EntityNotFoundException 用户不存在时抛出
     */
    User getUserById(Long id);
    
    /**
     * 保存用户信息
     *
     * @param user 用户信息
     * @return 保存后的用户信息
     */
    User saveUser(User user);
    
    /**
     * 更新用户信息
     *
     * @param id 用户ID
     * @param user 更新的用户信息
     * @return 更新后的用户信息
     */
    User updateUser(Long id, User user);
    
    /**
     * 根据ID删除用户
     *
     * @param id 用户ID
     */
    void removeUserById(Long id);
    
    /**
     * 批量删除用户
     *
     * @param ids 用户ID列表
     */
    void batchRemoveUsers(List<Long> ids);
}
```

## 接口设计规范

- 接口方法应简洁明确，体现业务语义
- 方法签名应稳定，避免频繁变更
- 复杂参数建议封装为对象传递
- 返回值类型应明确，避免使用 Object

## 其他建议

- 接口与实现分离，便于测试和扩展
- 方法注释应完整，说明异常情况
- 保持接口的单一职责原则
- 及时更新接口注释，与实现保持同步
