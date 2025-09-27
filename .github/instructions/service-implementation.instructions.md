---
applyTo: "*ServiceImpl.java"
---

# ServiceImpl 代码与注释编写规范

## ServiceImpl 规范

- 命名以 \*ServiceImpl 结尾，放在 service.impl 包
- 实现对应的 Service 接口
- 负责业务逻辑处理，调用 Repository 层
- 方法注释需说明业务含义、参数、返回值
- 使用 @Service 注解标记

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
 * 用户业务服务实现，处理用户相关的业务逻辑
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    
    /**
     * 分页查询用户列表
     *
     * @param pageRequest 分页参数
     * @param queryParam 查询条件
     * @return 用户分页数据
     */
    public Page<User> getPageUsers(Pageable pageRequest, UserQueryParam queryParam) {
        // 构建查询条件
        // 调用 Repository 进行查询
        return userRepository.findAll(pageRequest);
    }
    
    /**
     * 根据用户ID获取用户信息
     *
     * @param id 用户ID
     * @return 用户信息
     * @throws EntityNotFoundException 用户不存在时抛出
     */
    public User getUserById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("用户不存在"));
    }
    
    /**
     * 保存用户信息
     *
     * @param user 用户信息
     * @return 保存后的用户信息
     */
    @Transactional
    public User saveUser(User user) {
        // 业务逻辑校验
        validateUser(user);
        // 保存用户
        return userRepository.save(user);
    }
    
    /**
     * 更新用户信息
     *
     * @param id 用户ID
     * @param user 更新的用户信息
     * @return 更新后的用户信息
     */
    @Transactional
    public User updateUser(Long id, User user) {
        User existingUser = getUserById(id);
        // 更新字段
        BeanUtils.copyProperties(user, existingUser, "id", "createTime");
        return userRepository.save(existingUser);
    }
    
    /**
     * 根据ID删除用户
     *
     * @param id 用户ID
     */
    @Transactional
    public void removeUserById(Long id) {
        User user = getUserById(id);
        userRepository.delete(user);
    }
    
    /**
     * 批量删除用户
     *
     * @param ids 用户ID列表
     */
    @Transactional
    public void batchRemoveUsers(List<Long> ids) {
        userRepository.deleteAllById(ids);
    }
    
    /**
     * 校验用户信息
     *
     * @param user 用户信息
     */
    private void validateUser(User user) {
        // 业务校验逻辑
    }
}
```

## 业务逻辑规范

- 复杂业务逻辑应拆分为私有方法，提高可读性
- 涉及数据变更的方法必须添加 @Transactional 注解
- 异常处理应抛出具体的业务异常，便于上层处理
- 使用 @RequiredArgsConstructor 进行依赖注入

## 其他建议

- 避免在 Service 层直接处理 HTTP 相关逻辑
- 复杂查询条件建议封装为查询参数对象
- 批量操作时注意性能优化
- 及时更新方法注释，保持与代码同步
