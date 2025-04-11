package io.github.lishangbu.avalon.security.util;

import io.github.lishangbu.avalon.security.constant.JwtClaimConstants;
import io.github.lishangbu.avalon.security.core.UserPrincipal;
import io.github.lishangbu.avalon.security.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;
import java.util.Date;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.ResourceUtils;

@RequiredArgsConstructor
public class JwtUtils implements InitializingBean {

  private RSAPublicKey publicKey;

  private RSAPrivateKey privateKey;

  private final JwtProperties jwtProperties;

  /**
   * 基于认证对象生成访问jwt
   *
   * @param authentication 认证对象
   * @return JWT令牌
   */
  public String generateAccessTokenByAuthentication(Authentication authentication) {

    UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

    return Jwts.builder()
        .issuedAt(new Date())
        .expiration(
            Date.from(
                Instant.now()
                    .plus(
                        jwtProperties.getAccessTokenTtl(), jwtProperties.getAccessTokenTtlUnit())))
        .subject(userPrincipal.getUsername())
        .claim(JwtClaimConstants.USER_ID, userPrincipal.getId())
        .claim(
            JwtClaimConstants.AUTHORITIES,
            userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(",")))
        .signWith(privateKey)
        .compact();
  }

  /**
   * 生成刷新令牌
   *
   * @return
   */
  public String generateRefreshTokenByAuthentication() {

    return Jwts.builder()
        .issuedAt(new Date())
        .expiration(
            Date.from(
                Instant.now()
                    .plus(
                        jwtProperties.getRefreshTokenTtl(),
                        jwtProperties.getRefreshTokenTtlUnit())))
        .signWith(privateKey)
        .compact();
  }

  /**
   * 解析 JWT 并验证其签名。如果签名有效，它将返回 Claims 对象，可以从中提取 JWT 中的声明
   *
   * @param tokenValue JWT令牌值
   * @return Claims 对象，可以从中提取 JWT 中的声明
   */
  public Claims verifyJsonWebTokenByAuthentication(String tokenValue) {
    // 验证 JWT
    return Jwts.parser().verifyWith(publicKey).build().parseSignedClaims(tokenValue).getPayload();
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
    this.publicKey =
        loadPublicKey(
            new String(
                Files.readAllBytes(
                    ResourceUtils.getFile(jwtProperties.getPublicKeyPath()).toPath())));
    this.privateKey =
        loadPrivateKey(
            new String(
                Files.readAllBytes(
                    ResourceUtils.getFile(jwtProperties.getPrivateKeyPath()).toPath())));
  }
}
