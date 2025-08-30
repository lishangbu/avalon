package io.github.lishangbu.avalon.authorization.controller;

import com.github.pagehelper.PageInfo;
import com.github.pagehelper.PageParam;
import io.github.lishangbu.avalon.authorization.entity.User;
import io.github.lishangbu.avalon.authorization.model.UserDetail;
import io.github.lishangbu.avalon.authorization.service.UserService;
import io.github.lishangbu.avalon.oauth2.common.userdetails.UserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户控制器
 *
 * @author lishangbu
 * @since 2025/8/30
 */
@RequestMapping("/user")
@RestController
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;

  @GetMapping("/detail")
  public UserDetail getUserDetail(@AuthenticationPrincipal UserInfo user) {
    return userService.getUserDetailByUsername(user.getUsername()).orElse(null);
  }

  @GetMapping("/page")
  public PageInfo<User> getPage(PageParam pageParam, User user) {
    return userService.getPage(pageParam.getPageNum(), pageParam.getPageSize(), user);
  }
}
