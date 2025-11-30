package io.github.lishangbu.avalon.authorization.repository;

import io.github.lishangbu.avalon.authorization.entity.OauthAuthorization;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户认证信息表(oauth_authorization)表数据库访问层
 *
 * @author lishangbu
 * @since 2025/9/14
 */
@Repository
public interface OauthAuthorizationRepository
        extends ListCrudRepository<OauthAuthorization, String>,
        ListPagingAndSortingRepository<OauthAuthorization, String> {
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
   *
   * @param token token 值，可为上述任意一种 token
   * @return 匹配的认证信息
   */
  @Query(
      """
                      select * from oauth_authorization
                      where state = :token
                        or authorization_code_value = :token
                        or access_token_value = :token
                        or refresh_token_value = :token
                        or oidc_id_token_value = :token
                        or user_code_value = :token
                        or device_code_value = :token
      """)
  Optional<OauthAuthorization> findByToken(@Param("token") String token);
}
