package io.github.lishangbu.avalon.authorization.service.impl;

import io.github.lishangbu.avalon.authorization.entity.User;
import io.github.lishangbu.avalon.authorization.model.UserVO;
import io.github.lishangbu.avalon.authorization.repository.RoleRepository;
import io.github.lishangbu.avalon.authorization.repository.UserRepository;
import io.github.lishangbu.avalon.authorization.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 用户服务实现
 *
 * @author lishangbu
 * @since 2025/8/30
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
  /**
   * 用户数据存储层
   */
  private final UserRepository userRepository;

  private final RoleRepository roleRepository;

  /**
   * 根据用户名查询用户详情，包含基本信息、角色信息及个人资料
   *
   * @param username 用户名
   * @return 查询到的用户详情，未找到时返回Optional.empty()
   */
  @Override
  public Optional<UserVO> getUserByUsername(String username) {
    return userRepository
        .findByUsername(username)
        .map(
            user -> {
              UserVO userVO = new UserVO();
              BeanUtils.copyProperties(user, userVO);
              // 设置角色列表
              if (user.getUserRoles() != null && !user.getUserRoles().isEmpty()) {
                var roleIds =
                    user.getUserRoles().stream()
                        .map(relation -> relation.getRoleRef().getId())
                        .toList();
                var roles = roleRepository.findAllById(roleIds).stream().toList();
                userVO.setRoles(roles);
              }
              return userVO;
            });
  }
}
