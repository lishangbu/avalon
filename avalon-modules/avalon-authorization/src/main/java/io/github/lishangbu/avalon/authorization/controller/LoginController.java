package io.github.lishangbu.avalon.authorization.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录接口，登录使用的接口
 *
 * @author vains
 */
@RestController
@RequiredArgsConstructor
public class LoginController {

  @GetMapping("/getSmsCaptcha")
  public String getSmsCaptcha(String phone) {
    return "1234";
  }
}
