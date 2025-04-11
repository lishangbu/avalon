package io.github.lishangbu.avalon.auth.controller;

import io.github.lishangbu.avalon.auth.model.SignUpPayload;
import io.github.lishangbu.avalon.auth.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户控制器
 *
 * @author lishangbu
 * @since 2025/4/6
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;

  @PostMapping("/sign-up")
  public void saveUser(@RequestBody @Valid SignUpPayload payload) {
    userService.signUp(payload);
  }
}
