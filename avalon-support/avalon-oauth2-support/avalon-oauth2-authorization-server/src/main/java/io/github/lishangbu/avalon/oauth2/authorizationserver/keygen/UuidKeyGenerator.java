package io.github.lishangbu.avalon.oauth2.authorizationserver.keygen;

import java.util.UUID;
import org.springframework.security.crypto.keygen.StringKeyGenerator;

/**
 * UUID Key 生成器
 *
 * @author lishangbu
 * @since 2025/8/22
 */
public class UuidKeyGenerator implements StringKeyGenerator {
  @Override
  public String generateKey() {
    return UUID.randomUUID().toString().toLowerCase();
  }
}
