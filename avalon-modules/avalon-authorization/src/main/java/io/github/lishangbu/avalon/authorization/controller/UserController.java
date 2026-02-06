package io.github.lishangbu.avalon.authorization.controller;

import io.github.lishangbu.avalon.authorization.model.UserWithRoles;
import io.github.lishangbu.avalon.authorization.service.UserService;
import io.github.lishangbu.avalon.oauth2.common.userdetails.UserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/// 用户控制器
///
/// @author lishangbu
/// @since 2025/8/30
@RequestMapping("/user")
@RestController
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  /// 获取当前用户信息
  ///
  /// @param user 当前用户信息
  /// @return 用户信息
  @GetMapping("/info")
  public UserWithRoles getUserInfo(@AuthenticationPrincipal UserInfo user) {
    return userService.getUserByUsername(user.getUsername()).orElse(null);
  }
}
