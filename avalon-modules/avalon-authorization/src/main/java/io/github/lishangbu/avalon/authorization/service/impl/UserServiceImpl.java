package io.github.lishangbu.avalon.authorization.service.impl;

import io.github.lishangbu.avalon.authorization.mapper.UserMapper;
import io.github.lishangbu.avalon.authorization.model.UserVO;
import io.github.lishangbu.avalon.authorization.service.UserService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/// 用户服务实现
///
/// 提供基于用户名的用户信息查询，返回包含角色与个人资料的 UserVO
///
/// @author lishangbu
/// @since 2025/8/30
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
  /// 用户数据访问 Mapper
  private final UserMapper userMapper;

  /// 根据用户名查询用户详情，包含基本信息、角色信息及个人资料
  ///
  /// @param username 用户名
  /// @return 查询到的用户详情，未找到时返回 Optional.empty()
  @Override
  public Optional<UserVO> getUserByUsername(String username) {
    return userMapper.selectByUsername(username);
  }
}
