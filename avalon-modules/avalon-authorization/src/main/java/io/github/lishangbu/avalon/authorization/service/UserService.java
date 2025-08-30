package io.github.lishangbu.avalon.authorization.service;

import com.github.pagehelper.PageInfo;
import io.github.lishangbu.avalon.authorization.entity.User;
import io.github.lishangbu.avalon.authorization.model.UserDetail;
import java.util.Optional;

/**
 * 用户服务
 *
 * @author lishangbu
 * @since 2025/8/30
 */
public interface UserService {

  /**
   * 分页查询用户信息
   *
   * @param pageNum 当前分页
   * @param pageSize 分页大小
   * @param user 查询条件
   * @return 包含分页的用户信息
   */
  PageInfo<User> getPage(Integer pageNum, Integer pageSize, User user);

  /**
   * 根据用户名查询用户详情，包含基本信息、角色信息及个人资料
   *
   * @param username 用户名
   * @return 查询到的用户详情，未找到时返回Optional.empty()
   */
  Optional<UserDetail> getUserDetailByUsername(String username);
}
