package io.github.lishangbu.avalon.authorization.mapper;

import io.github.lishangbu.avalon.authorization.entity.Oauth2Authorization;
import java.util.Optional;
import org.apache.ibatis.annotations.Param;

/**
 * 用户认证信息表(oauth2_authorization)表数据库访问层
 *
 * @author lishangbu
 * @since 2025/08/20
 */
public interface Oauth2AuthorizationMapper {

  /**
   * 通过id查询单条用户认证信息表数据
   *
   * @param id 主键
   * @return 可选的用户认证信息表
   */
  Optional<Oauth2Authorization> selectById(String id);

  /**
   * 新增用户认证信息表
   *
   * @param oauth2Authorization 实例对象
   * @return 影响行数
   */
  int insert(Oauth2Authorization oauth2Authorization);

  /**
   * 修改用户认证信息表
   *
   * @param oauth2Authorization 实例对象
   * @return 影响行数
   */
  int updateById(Oauth2Authorization oauth2Authorization);

  /**
   * 通过id删除用户认证信息表
   *
   * @param id 主键
   * @return 影响行数
   */
  int deleteById(String id);

  /**
   * 通过状态查询用户认证信息表
   *
   * @param state 状态
   * @return 可选的用户认证信息表
   */
  Optional<Oauth2Authorization> selectByState(@Param("state") String state);

  /**
   * 通过授权码查询用户认证信息表
   *
   * @param authorizationCodeValue 授权码值
   * @return 可选的用户认证信息表
   */
  Optional<Oauth2Authorization> selectByAuthorizationCodeValue(
      @Param("authorizationCodeValue") String authorizationCodeValue);

  /**
   * 通过访问令牌查询用户认证信息表
   *
   * @param accessTokenValue 访问令牌值
   * @return 可选的用户认证信息表
   */
  Optional<Oauth2Authorization> selectByAccessTokenValue(
      @Param("accessTokenValue") String accessTokenValue);

  /**
   * 通过刷新令牌查询用户认证信息表
   *
   * @param refreshTokenValue 刷新令牌值
   * @return 可选的用户认证信息表
   */
  Optional<Oauth2Authorization> selectByRefreshTokenValue(
      @Param("refreshTokenValue") String refreshTokenValue);

  /**
   * 通过OIDC ID 令牌查询用户认证信息表
   *
   * @param oidcIdTokenValue OIDC ID 令牌
   * @return 可选的用户认证信息表
   */
  Optional<Oauth2Authorization> selectByOidcIdTokenValue(
      @Param("oidcIdTokenValue") String oidcIdTokenValue);

  /**
   * 通过用户码查询用户认证信息表
   *
   * @param userCodeValue 用户码的值
   * @return 可选的用户认证信息表
   */
  Optional<Oauth2Authorization> selectByUserCodeValue(@Param("userCodeValue") String userCodeValue);

  /**
   * 通过设备码的值查询用户认证信息表
   *
   * @param deviceCodeValue 设备码的值
   * @return 可选的用户认证信息表
   */
  Optional<Oauth2Authorization> selectByDeviceCodeValue(
      @Param("deviceCodeValue") String deviceCodeValue);

  /**
   * 通过令牌值查询用户认证信息表
   *
   * @param token 令牌值
   * @return 可选的用户认证信息表
   */
  Optional<Oauth2Authorization>
      selectByStateOrAuthorizationCodeValueOrAccessTokenValueOrRefreshTokenValueOrOidcIdTokenValueOrUserCodeValueOrDeviceCodeValue(
          @Param("token") String token);
}
