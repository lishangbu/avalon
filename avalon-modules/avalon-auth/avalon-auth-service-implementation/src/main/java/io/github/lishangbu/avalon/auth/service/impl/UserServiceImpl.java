package io.github.lishangbu.avalon.auth.service.impl;

import io.github.lishangbu.avalon.auth.entity.User;
import io.github.lishangbu.avalon.auth.entity.UserRoleRelation;
import io.github.lishangbu.avalon.auth.model.SignUpPayload;
import io.github.lishangbu.avalon.auth.model.UserDTO;
import io.github.lishangbu.avalon.auth.repository.RoleRepository;
import io.github.lishangbu.avalon.auth.repository.UserRepository;
import io.github.lishangbu.avalon.auth.service.UserService;
import io.github.lishangbu.avalon.security.core.UserPrincipal;
import java.util.Optional;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * 用户服务
 *
 * @author lishangbu
 * @since 2025/4/9
 */
@Service
public class UserServiceImpl implements UserService, UserDetailsService {
  private final PasswordEncoder passwordEncoder;
  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final JdbcAggregateTemplate jdbcAggregateTemplate;

  public UserServiceImpl(
      PasswordEncoder passwordEncoder,
      UserRepository userRepository,
      RoleRepository roleRepository,
      JdbcAggregateTemplate jdbcAggregateTemplate) {
    this.passwordEncoder = passwordEncoder;
    this.userRepository = userRepository;
    this.roleRepository = roleRepository;
    this.jdbcAggregateTemplate = jdbcAggregateTemplate;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    Optional<UserDTO> userOptional = userRepository.findByUsername(username);
    if (userOptional.isPresent()) {
      UserDTO user = userOptional.get();
      return new UserPrincipal(
          user.id(),
          username,
          user.password(),
          AuthorityUtils.createAuthorityList(user.roleCodes().split(",")));
    } else {
      throw new UsernameNotFoundException("用户名或密码错误");
    }
  }

  /**
   * 注册用户
   *
   * @param payload 用户注册数据
   */
  @Transactional(rollbackFor = Exception.class)
  @Override
  public void signUp(SignUpPayload payload) {
    Assert.isTrue(!userRepository.existsByUsername(payload.username()), "用户已存在");
    User user = new User();
    user.setUsername(payload.username());
    user.setPassword(passwordEncoder.encode(payload.password()));
    jdbcAggregateTemplate.insert(user);
    // 保存用户角色关系
    roleRepository
        .findByCode(payload.roleCode())
        .ifPresent(
            role -> {
              UserRoleRelation userRoleRelation = new UserRoleRelation();
              userRoleRelation.setUserId(user.getId());
              userRoleRelation.setRoleId(role.getId());
              jdbcAggregateTemplate.insert(userRoleRelation);
            });
  }
}
