package io.github.lishangbu.avalon.authorization.service.impl;

import io.github.lishangbu.avalon.authorization.model.UserWithRoles;
import io.github.lishangbu.avalon.authorization.repository.UserRepository;
import io.github.lishangbu.avalon.authorization.service.UserService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/// 用户服务实现
///
/// @author lishangbu
/// @since 2025/8/30
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    /// 根据用户名查询用户详情，包含基本信息、角色信息及个人资料
    ///
    /// @param username 用户名
    /// @return 查询到的用户详情，未找到时返回Optional.empty()
    @Override
    public Optional<UserWithRoles> getUserByUsername(String username) {
        return userRepository.findUserWithRolesByUsername(username).map(UserWithRoles::new);
    }
}
