package io.github.lishangbu.avalon.authorization.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 获取验证码返回
 *
 * @author lishangbu
 * @since 2025/8/21
 */
@Data
@AllArgsConstructor
public class CaptchaResult {

  /** 验证码id */
  private String captchaId;

  /** 验证码的值 */
  private String code;

  /** 图片验证码的base64值 */
  private String imageData;
}
