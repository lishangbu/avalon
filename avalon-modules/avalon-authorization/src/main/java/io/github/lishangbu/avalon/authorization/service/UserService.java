package io.github.lishangbu.avalon.authorization.service;

import io.github.lishangbu.avalon.authorization.model.UserWithRoles;
import io.github.lishangbu.avalon.authorization.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/// 用户服务
///
/// 提供用户相关的查询与管理操作
///
/// @author lishangbu
/// @since 2025/8/30
public interface UserService {

    /// 根据用户名/手机号/邮箱查询用户详情，包含基本信息、角色信息及个人资料
    ///
    /// @param username 登录账号
    /// @return 查询到的用户详情，未找到时返回 Optional.empty()
    Optional<UserWithRoles> getUserByUsername(String username);

    /// 根据条件分页查询用户。
    Page<User> getPageByCondition(User user, Pageable pageable);

    /// 根据条件查询用户列表。
    List<User> listByCondition(User user);

    /// 根据 ID 查询用户。
    Optional<User> getById(Long id);

    /// 新增用户。
    User save(User user);

    /// 更新用户。
    User update(User user);

    /// 根据 ID 删除用户。
    void removeById(Long id);
}
