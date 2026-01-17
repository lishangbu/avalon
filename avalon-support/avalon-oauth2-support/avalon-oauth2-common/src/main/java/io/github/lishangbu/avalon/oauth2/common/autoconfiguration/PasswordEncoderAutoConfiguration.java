package io.github.lishangbu.avalon.oauth2.common.autoconfiguration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/// 密码加密器自动装配
///
/// 提供默认的 PasswordEncoder，除非容器中已有其他实现
///
/// @author lishangbu
/// @since 2025/8/17
@AutoConfiguration
public class PasswordEncoderAutoConfiguration {
  @Bean
  @ConditionalOnMissingBean
  public PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }
}
