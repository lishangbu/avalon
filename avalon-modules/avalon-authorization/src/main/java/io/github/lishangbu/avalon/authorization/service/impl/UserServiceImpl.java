package io.github.lishangbu.avalon.authorization.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import io.github.lishangbu.avalon.authorization.entity.User;
import io.github.lishangbu.avalon.authorization.mapper.UserMapper;
import io.github.lishangbu.avalon.authorization.model.UserDetail;
import io.github.lishangbu.avalon.authorization.service.UserService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现
 *
 * @author lishangbu
 * @since 2025/8/30
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
  private final UserMapper userMapper;

  @Override
  public PageInfo<User> getPage(Integer pageNum, Integer pageSize, User user) {
    return PageHelper.startPage(pageNum, pageSize)
        .doSelectPageInfo(() -> userMapper.selectAll(user));
  }

  /**
   * 根据用户名查询用户详情，包含基本信息、角色信息及个人资料
   *
   * @param username 用户名
   * @return 查询到的用户详情，未找到时返回Optional.empty()
   */
  @Override
  public Optional<UserDetail> getUserDetailByUsername(String username) {
    return userMapper.selectUserDetailByUsername(username);
  }
}
