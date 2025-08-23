package io.github.lishangbu.avalon.oauth2.authorizationserver.autoconfiguration;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import io.github.lishangbu.avalon.oauth2.common.properties.Oauth2Properties;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;

/**
 * JWKSource自动装配
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
   * An instance of com.nimbusds.jose.jwk.source.JWKSource for signing access tokens.
   *
   * @return
   */
  @Bean
  @ConditionalOnMissingBean
  public JWKSource<SecurityContext> jwkSource() {
    if (publicKey == null || privateKey == null) {
      log.warn("未配置公钥或私钥，使用随机生成的密钥对，重启后之前签发的token将无法解析");
      KeyPair keyPair = generateRsaKey();
      publicKey = (RSAPublicKey) keyPair.getPublic();
      privateKey = (RSAPrivateKey) keyPair.getPrivate();
    }
    RSAKey rsaKey =
        new RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyID(UUID.randomUUID().toString())
            .build();
    JWKSet jwkSet = new JWKSet(rsaKey);
    return new ImmutableJWKSet<>(jwkSet);
  }

  /**
   * An instance of java.security.KeyPair with keys generated on startup used to create the
   * JWKSource above.
   *
   * @return
   */
  private KeyPair generateRsaKey() {
    KeyPair keyPair;
    try {
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(2048);
      keyPair = keyPairGenerator.generateKeyPair();
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
    return keyPair;
  }

  /**
   * 从公钥字符串加载公钥
   *
   * @param publicKeyContent
   * @return
   * @throws Exception
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
   * 从私钥字符串加载私钥
   *
   * @param privateKeyContent
   * @return
   * @throws Exception
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

  @Override
  public void afterPropertiesSet() throws Exception {
    try (InputStream inputStream =
        resourceLoader.getResource(oauth2Properties.getJwtPublicKeyLocation()).getInputStream()) {
      this.publicKey = loadPublicKey(new String(inputStream.readAllBytes()));
    } catch (IOException e) {
      log.error("公钥读取失败,无法从[{}]检索到公钥", oauth2Properties.getJwtPublicKeyLocation());
    }
    try (InputStream inputStream =
        resourceLoader.getResource(oauth2Properties.getJwtPrivateKeyLocation()).getInputStream()) {
      this.privateKey = loadPrivateKey(new String(inputStream.readAllBytes()));
    } catch (IOException e) {
      log.error("私钥读取失败,无法从[{}]检索到私钥", oauth2Properties.getJwtPrivateKeyLocation());
    }
  }
}
