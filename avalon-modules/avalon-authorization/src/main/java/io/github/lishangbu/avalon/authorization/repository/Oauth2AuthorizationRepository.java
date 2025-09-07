package io.github.lishangbu.avalon.authorization.repository;

import io.github.lishangbu.avalon.authorization.entity.OauthAuthorization;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 用户认证信息表(oauth_authorization)表数据库访问层
 *
 * @author lishangbu
 * @since 2025/9/14
 */
@Repository
public interface Oauth2AuthorizationRepository extends JpaRepository<OauthAuthorization, String> {
  /**
   * 根据 state 查询认证信息
   *
   * @param state 状态码
   * @return 匹配的认证信息
   */
  Optional<OauthAuthorization> findByState(String state);

  /**
   * 根据授权码查询认证信息
   *
   * @param authorizationCode 授权码
   * @return 匹配的认证信息
   */
  Optional<OauthAuthorization> findByAuthorizationCodeValue(String authorizationCode);

  /**
   * 根据访问令牌查询认证信息
   *
   * @param accessToken 访问令牌
   * @return 匹配的认证信息
   */
  Optional<OauthAuthorization> findByAccessTokenValue(String accessToken);

  /**
   * 根据刷新令牌查询认证信息
   *
   * @param refreshToken 刷新令牌
   * @return 匹配的认证信息
   */
  Optional<OauthAuthorization> findByRefreshTokenValue(String refreshToken);

  /**
   * 根据 OIDC ID Token 查询认证信息
   *
   * @param idToken OIDC ID Token
   * @return 匹配的认证信息
   */
  Optional<OauthAuthorization> findByOidcIdTokenValue(String idToken);

  /**
   * 根据用户码查询认证信息
   *
   * @param userCode 用户码
   * @return 匹配的认证信息
   */
  Optional<OauthAuthorization> findByUserCodeValue(String userCode);

  /**
   * 根据设备码查询认证信息
   *
   * @param deviceCode 设备码
   * @return 匹配的认证信息
   */
  Optional<OauthAuthorization> findByDeviceCodeValue(String deviceCode);

  /**
   * 根据多种 token 字段联合查询认证信息，支持
   * state、authorizationCode、accessToken、refreshToken、idToken、userCode、deviceCode 任意一种 token。
   * 查询语句为多行文本块，便于维护和阅读。
   *
   * @param token token 值，可为上述任意一种 token
   * @return 匹配的认证信息
   */
  @Query(
      """
      select a from OauthAuthorization a
      where a.state = :token
        or a.authorizationCodeValue = :token
        or a.accessTokenValue = :token
        or a.refreshTokenValue = :token
        or a.oidcIdTokenValue = :token
        or a.userCodeValue = :token
        or a.deviceCodeValue = :token
      """)
  Optional<OauthAuthorization>
      findByStateOrAuthorizationCodeValueOrAccessTokenValueOrRefreshTokenValueOrOidcIdTokenValueOrUserCodeValueOrDeviceCodeValue(
          @Param("token") String token);
}
