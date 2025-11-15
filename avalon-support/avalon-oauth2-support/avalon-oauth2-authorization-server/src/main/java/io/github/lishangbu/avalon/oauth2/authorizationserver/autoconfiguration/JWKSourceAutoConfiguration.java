package io.github.lishangbu.avalon.oauth2.authorizationserver.autoconfiguration;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import io.github.lishangbu.avalon.oauth2.common.properties.Oauth2Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.UUID;

/**
 * JWKSource 自动装配
 *
 * <p>负责提供用于签名的 JWKSource 实例，支持从配置加载公/私钥，若未配置则回退为运行时生成的密钥对
 *
 * <p>行为要点：
 *
 * <ul>
 *   <li>密钥在首次使用时懒初始化，线程安全
 *   <li>优先使用公钥 thumbprint 作为 kid，保证 kid 稳定可复现，避免验证不稳定
 *   <li>Spring Authorization Server 会自动从 JWKSource 提取公钥信息供 /.well-known/jwks.json 端点使用
 *   <li>支持从 classpath 或文件系统加载 PEM 格式的公私钥
 *   <li>加载失败时自动回退为随机生成的密钥对，并记录警告
 * </ul>
 *
 * @author lishangbu
 * @since 2025/8/17
 */
@Slf4j
@AutoConfiguration
@RequiredArgsConstructor
public class JWKSourceAutoConfiguration implements InitializingBean {

  private final Oauth2Properties oauth2Properties;

  private final ResourceLoader resourceLoader;

  private RSAPublicKey publicKey;

  private RSAPrivateKey privateKey;

  /**
   * 缓存的 JWKSet，包含私钥，用于 JWT 签名
   *
   * <p>使用 volatile 保证多线程环境下的可见性，配合 synchronized 方法实现线程安全的懒加载
   */
  private volatile JWKSet jwkSet;

  /**
   * 返回用于签名的 JWKSource
   *
   * <p>方法保证密钥已初始化（从配置加载或运行时生成），并返回一个不可变的 JWKSet 供框架选择签名密钥 Spring Authorization Server 会自动处理
   * /.well-known/jwks.json 端点，从此 JWKSource 提取公钥信息
   *
   * @return 包含私钥的不可变 JWKSource，用于 Authorization Server 的 JWT 签名
   */
  @Bean
  @ConditionalOnMissingBean
  public JWKSource<SecurityContext> jwkSource() {
    ensureKeysInitialized();
    return new ImmutableJWKSet<>(jwkSet);
  }

  /**
   * 确保密钥与 JWKSet 已经初始化（线程安全的懒初始化）
   *
   * <p>该方法执行以下操作：
   *
   * <ol>
   *   <li>检查是否已初始化，避免重复初始化
   *   <li>若公私钥为空，使用随机生成的密钥对并记录警告
   *   <li>计算 kid（优先使用公钥 thumbprint，失败时使用随机 UUID）
   *   <li>构建包含私钥的 JWKSet（框架会自动过滤私钥信息对外暴露）
   * </ol>
   */
  private synchronized void ensureKeysInitialized() {
    if (jwkSet != null) {
      return;
    }

    // 如果没有通过配置加载到公私钥，则生成随机密钥对
    if (publicKey == null || privateKey == null) {
      log.warn("未配置公钥或私钥，使用随机生成的密钥对，重启后之前签发的 token 将无法解析");
      KeyPair keyPair = generateRsaKey();
      publicKey = (RSAPublicKey) keyPair.getPublic();
      privateKey = (RSAPrivateKey) keyPair.getPrivate();
    }

    // 计算 kid：优先使用公钥 thumbprint，计算失败时回退为随机 UUID
    String kid;
    try {
      kid = new RSAKey.Builder(publicKey).build().computeThumbprint().toString();
      log.debug("使用公钥 thumbprint 作为 kid: {}", kid);
    } catch (JOSEException e) {
      kid = UUID.randomUUID().toString();
      log.warn("计算公钥 thumbprint 失败，回退为随机 kid: {}", kid, e);
    }

    // 使用默认算法 RS256，并标记为签名用途
    JWSAlgorithm alg = JWSAlgorithm.RS256;

    // 构建 JWK（包含私钥）
    RSAKey rsaKey =
      new RSAKey.Builder(publicKey).privateKey(privateKey).keyID(kid).algorithm(alg).build();

    this.jwkSet = new JWKSet(rsaKey);

    log.info("JWKSet 初始化完成，kid: {}, 算法: {}", kid, alg);
  }

  /**
   * 生成用于签名的 RSA 密钥对
   *
   * <p>使用 2048 位 RSA 算法生成密钥对，适用于 JWT 签名场景
   *
   * @return 生成的 RSA KeyPair，公钥和私钥均为 RSA 实现
   * @throws IllegalStateException 当密钥生成失败时抛出
   */
  private KeyPair generateRsaKey() {
    KeyPair keyPair;
    try {
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(2048);
      keyPair = keyPairGenerator.generateKeyPair();
      log.debug("成功生成 2048 位 RSA 密钥对");
    } catch (Exception ex) {
      log.error("生成 RSA 密钥对失败", ex);
      throw new IllegalStateException("无法生成 RSA 密钥对", ex);
    }
    return keyPair;
  }

  /**
   * 从 PEM 格式的公钥字符串加载 RSA 公钥实例
   *
   * <p>支持标准的 PEM 格式，自动移除头尾行和空白字符
   *
   * @param publicKeyContent 公钥的 PEM 文本，允许包含头尾行和换行
   * @return 解析得到的 {@link RSAPublicKey}
   * @throws NoSuchAlgorithmException 当 RSA 算法不可用时抛出
   * @throws InvalidKeySpecException 当密钥格式不符合 X.509 编码时抛出
   */
  private static RSAPublicKey loadPublicKey(String publicKeyContent)
    throws NoSuchAlgorithmException, InvalidKeySpecException {
    publicKeyContent =
      publicKeyContent
        .replace("-----BEGIN PUBLIC KEY-----", "")
        .replace("-----END PUBLIC KEY-----", "")
        .replaceAll("\\s", "");
    byte[] encoded = java.util.Base64.getDecoder().decode(publicKeyContent);
    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    return (RSAPublicKey)
      keyFactory.generatePublic(new java.security.spec.X509EncodedKeySpec(encoded));
  }

  /**
   * 从 PEM 格式的私钥字符串加载 RSA 私钥实例
   *
   * <p>支持标准的 PEM 格式（PKCS#8），自动移除头尾行和空白字符
   *
   * @param privateKeyContent 私钥的 PEM 文本，允许包含头尾行和换行
   * @return 解析得到的 {@link RSAPrivateKey}
   * @throws NoSuchAlgorithmException 当 RSA 算法不可用时抛出
   * @throws InvalidKeySpecException 当密钥格式不符合 PKCS#8 编码时抛出
   */
  private static RSAPrivateKey loadPrivateKey(String privateKeyContent)
    throws NoSuchAlgorithmException, InvalidKeySpecException {
    privateKeyContent =
      privateKeyContent
        .replace("-----BEGIN PRIVATE KEY-----", "")
        .replace("-----END PRIVATE KEY-----", "")
        .replaceAll("\\s", "");
    byte[] encoded = java.util.Base64.getDecoder().decode(privateKeyContent);
    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    return (RSAPrivateKey)
      keyFactory.generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(encoded));
  }

  /**
   * 初始化时尝试从配置的资源位置加载 RSA 公钥和私钥
   *
   * <p>优先使用 oauth2Properties 中的 jwtPublicKeyLocation 和 jwtPrivateKeyLocation 指定的资源
   * 若两者均成功加载则使用加载的密钥；若任一密钥缺失或解析失败则回退为随机生成的密钥对， 并记录警告，注意重启后之前签发的 token 将不可解析
   *
   * <p>支持的资源位置格式：
   *
   * <ul>
   *   <li>classpath: classpath:rsa/public.key
   *   <li>文件系统: file:/path/to/public.key
   *   <li>相对路径: rsa/public.key（相对于 classpath）
   * </ul>
   */
  @Override
  public void afterPropertiesSet() {
    final String jwtPublicKeyLocation = oauth2Properties.getJwtPublicKeyLocation();
    final String jwtPrivateKeyLocation = oauth2Properties.getJwtPrivateKeyLocation();
    // 密钥缺失或解析失败，标记为未加载状态，将在首次使用时生成随机密钥对
    if (ObjectUtils.isEmpty(jwtPublicKeyLocation) && ObjectUtils.isEmpty(jwtPrivateKeyLocation)) {
      log.warn("未配置公钥和私钥路径，将在首次使用时生成随机密钥对");
      return;
    }

    RSAPublicKey loadedPublic = null;
    RSAPrivateKey loadedPrivate = null;

    // 尝试加载公钥
    if (StringUtils.hasText(jwtPublicKeyLocation)) {
      try {
        Resource resource = resourceLoader.getResource(jwtPublicKeyLocation);
        if (resource.exists() && resource.isReadable()) {
          try (InputStream inputStream = resource.getInputStream()) {
            String content =
              new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            loadedPublic = loadPublicKey(content);
            log.debug("从 [{}] 成功加载公钥", jwtPublicKeyLocation);
          }
        } else {
          log.error("公钥资源不存在或不可读: {}", jwtPublicKeyLocation);
        }
      } catch (IOException e) {
        log.error("公钥读取失败，无法从 [{}] 检索到有效公钥: {}", jwtPublicKeyLocation, e.getMessage());
      } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
        log.error("公钥解析失败，无法从 [{}] 解析到有效公钥: {}", jwtPublicKeyLocation, e.getMessage());
      }
    } else {
      log.warn("公钥或私钥加载不完整，将在首次使用时生成随机密钥对");
    }

    // 尝试加载私钥
    if (StringUtils.hasText(jwtPrivateKeyLocation)) {
      try {
        var resource = resourceLoader.getResource(jwtPrivateKeyLocation);
        if (resource.exists() && resource.isReadable()) {
          try (InputStream inputStream = resource.getInputStream()) {
            String content =
              new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            loadedPrivate = loadPrivateKey(content);
            log.debug("从 [{}] 成功加载私钥", jwtPrivateKeyLocation);
          }
        } else {
          log.error("私钥资源不存在或不可读: {}", jwtPrivateKeyLocation);
        }
      } catch (IOException e) {
        log.error("私钥读取失败，无法从 [{}] 检索到有效私钥: {}", jwtPrivateKeyLocation, e.getMessage());
      } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
        log.error("私钥解析失败，无法从 [{}] 解析到有效私钥: {}", jwtPrivateKeyLocation, e.getMessage());
      }
    } else {
      log.warn("私钥加载不完整，将在首次使用时生成随机密钥对");
    }

    // 若公私钥均已加载则直接使用
    if (loadedPublic != null && loadedPrivate != null) {
      this.publicKey = loadedPublic;
      this.privateKey = loadedPrivate;
      log.debug("成功从配置加载公私钥对");
    }
  }
}
