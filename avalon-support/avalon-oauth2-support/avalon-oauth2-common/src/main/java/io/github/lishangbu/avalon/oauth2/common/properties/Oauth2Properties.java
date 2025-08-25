package io.github.lishangbu.avalon.oauth2.common.properties;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Oauth2安全配置
 *
 * @author lishangbu
 * @since 2025/8/17
 */
@Data
@AutoConfiguration
@ConfigurationProperties(prefix = Oauth2Properties.PREFIX)
public class Oauth2Properties {
  public static final String PREFIX = "oauth2";

  /** 不需要认证的路径 */
  private List<String> ignoreUrls = new ArrayList<>();

  /**
   * 设置token签发地址(http(s)://{ip}:{port}/context-path, http(s)://domain.com/context-path)
   * 如果需要通过ip访问这里就是ip，如果是有域名映射就填域名，通过什么方式访问该服务这里就填什么
   */
  private String issuerUrl;

  /** JWT公钥位置 */
  private String jwtPublicKeyLocation = "classpath:rsa/public.key";

  /** JWT私钥位置 */
  private String jwtPrivateKeyLocation = "classpath:rsa/private.key";
}
