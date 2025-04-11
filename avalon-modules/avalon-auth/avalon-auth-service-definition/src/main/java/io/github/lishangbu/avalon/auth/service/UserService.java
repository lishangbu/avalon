package io.github.lishangbu.avalon.auth.service;

import io.github.lishangbu.avalon.auth.model.SignUpPayload;

/**
 * 用户服务
 *
 * @author lishangbu
 * @since 2025/4/9
 */
public interface UserService {

  /**
   * 注册用户
   *
   * @param payload 用户注册数据
   */
  void signUp(SignUpPayload payload);
}
