package io.github.lishangbu.avalon.authorization.service.impl;

import io.github.lishangbu.avalon.authorization.entity.Role;
import io.github.lishangbu.avalon.authorization.service.UserService;
import io.github.lishangbu.avalon.oauth2.common.userdetails.UserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/// 用户详细信息服务
///
/// 用于加载用户的认证信息（UserDetails），将数据库中的用户与角色转换为 Spring Security 的 UserDetails
///
/// @author lishangbu
/// @since 2025/8/17
@Service
@RequiredArgsConstructor
public class DefaultUserDetailsService implements UserDetailsService {
  private final UserService userService;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    return userService
        .getUserByUsername(username)
        .map(
            user ->
                new UserInfo(
                    user.getUsername(),
                    user.getPassword(),
                    CollectionUtils.isEmpty(user.getRoles())
                        ? AuthorityUtils.NO_AUTHORITIES
                        : AuthorityUtils.createAuthorityList(
                            user.getRoles().stream().map(Role::getCode).toList())))
        .orElseThrow(() -> new UsernameNotFoundException("用户名或密码错误"));
  }
}
