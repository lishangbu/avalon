package io.github.lishangbu.avalon.oauth2.common.properties;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OAuth2 安全配置属性类
 *
 * <p>封装 OAuth2 授权服务器的核心配置参数，支持通过 application.yml 或 application.properties 进行配置 配置前缀为
 * oauth2，包含认证路径控制、JWT 密钥管理、Token 签发等关键设置
 *
 * <p>配置示例：
 *
 * <pre>{@code
 * oauth2:
 *   issuer-url: http://localhost:8080
 *   ignore-urls:
 *     - /public/**
 *     - /health
 *   username-parameter-name: username
 *   password-parameter-name: password
 *   jwt-public-key-location: classpath:rsa/public.key
 *   jwt-private-key-location: classpath:rsa/private.key
 * }</pre>
 *
 * @author lishangbu
 * @since 2025/8/17
 */
@Data
@AutoConfiguration
@ConfigurationProperties(prefix = Oauth2Properties.PREFIX)
public class Oauth2Properties {
  /** 配置属性前缀，用于绑定 application.yml 中的 oauth2 配置段 */
  public static final String PREFIX = "oauth2";

  /**
   * 免认证路径列表，匹配的请求路径将跳过 OAuth2 安全验证
   *
   * <p>支持 Ant 路径匹配模式，如 /public/**、/health、/actuator/* 默认为空列表，表示所有路径都需要认证
   */
  private List<String> ignoreUrls = new ArrayList<>();

  /**
   * 用户名参数名称，用于密码授权模式中的用户名字段
   *
   * <p>默认值为 username，可根据前端表单字段名称进行自定义
   */
  private String usernameParameterName = "username";

  /**
   * 密码参数名称，用于密码授权模式中的密码字段
   *
   * <p>默认值为 password，可根据前端表单字段名称进行自定义
   */
  private String passwordParameterName = "password";

  /**
   * JWT Token 签发者地址，用于标识 Token 的颁发机构
   *
   * <p>格式要求：http(s)://{ip}:{port}/context-path 或 http(s)://domain.com/context-path
   * 必须与客户端访问服务的实际地址保持一致，用于 Token 验证和 OIDC Discovery
   *
   * <p>配置建议：
   *
   * <ul>
   *   <li>开发环境：http://localhost:8080
   *   <li>生产环境：https://auth.example.com
   *   <li>内网部署：http://192.168.1.100:8080
   * </ul>
   */
  private String issuerUrl;

  /**
   * JWT 公钥文件位置，用于 Token 签名验证
   *
   * <p>文件不存在时，则随机生成密钥对，强烈建议指定自己的密钥对并做好私钥保护
   *
   * <p>支持 classpath 和文件系统路径，默认使用 RSA 算法 公钥文件格式为 PEM 格式，与私钥配对使用
   *
   * <p>可通过 {@code scripts/rsa-key-pair.sh} 脚本生成密钥对
   */
  private String jwtPublicKeyLocation;

  /**
   * JWT 私钥文件位置，用于 Token 签名生成
   *
   * <p>支持 classpath 和文件系统路径，默认使用 RSA 算法 私钥文件格式为 PEM 格式，需要妥善保管确保安全
   *
   * <p>生产环境建议将私钥存放在安全的文件系统路径中
   */
  private String jwtPrivateKeyLocation;
}
