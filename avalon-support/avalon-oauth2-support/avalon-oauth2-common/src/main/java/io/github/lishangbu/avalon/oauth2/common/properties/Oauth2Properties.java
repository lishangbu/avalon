package io.github.lishangbu.avalon.oauth2.common.properties;

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

  /** 登录处理地址 */
  private String loginProcessingUrl = "/login";

  /** 登出处理地址 */
  private String logoutProcessingUrl = "/logout";

  /** 登录页面地址 注意：不是前后端分离的项目不要写完整路径，当前项目部署的IP也不行！！！ 错误e.g. http://当前项目IP:当前项目端口/login */
  private String loginPageUrl = "/login";

  /** 登出页面地址 注意：不是前后端分离的项目不要写完整路径，当前项目部署的IP也不行！！！ 错误e.g. http://当前项目IP:当前项目端口/logout */
  private String logoutPageUrl = "/logout";

  /** 授权确认页面 注意：不是前后端分离的项目不要写完整路径，当前项目部署的IP也不行！！！ 错误e.g. http://当前项目IP:当前项目端口/activated */
  private String consentPageUrl = "/oauth2/consent";

  /** 授权码验证页面 注意：不是前后端分离的项目不要写完整路径，当前项目部署的IP也不行！！！ 错误e.g. http://当前项目IP:当前项目端口/activated */
  private String deviceActivateUrl = "/activate";

  /** 授权码验证成功后页面 注意：不是前后端分离的项目不要写完整路径，当前项目部署的IP也不行！！！ 错误e.g. http://当前项目IP:当前项目端口/activated */
  private String deviceActivatedUrl = "/activated";

  /** 不需要认证的路径 */
  private List<String> ignoreUrls;

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
