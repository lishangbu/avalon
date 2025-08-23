package io.github.lishangbu.avalon.authorization.mapper;

import io.github.lishangbu.avalon.authorization.entity.Oauth2AuthorizationConsent;
import java.util.Optional;
import org.apache.ibatis.annotations.Param;

/**
 * 用户授权确认表(oauth2_authorization_consent)表数据库访问层
 *
 * @author lishangbu
 * @since 2025/08/19
 */
public interface Oauth2AuthorizationConsentMapper {

  /**
   * 新增用户授权确认表
   *
   * @param oauth2AuthorizationConsent 实例对象
   * @return 影响行数
   */
  int insert(Oauth2AuthorizationConsent oauth2AuthorizationConsent);

  /**
   * 通过registeredClientId和principalName修改用户授权确认表
   *
   * @param oauth2AuthorizationConsent 实例对象
   * @return 影响行数
   */
  int updateByByRegisteredClientIdAndPrincipalName(
      Oauth2AuthorizationConsent oauth2AuthorizationConsent);

  /**
   * 通过registeredClientId和principalName删除用户授权确认表
   *
   * @param registeredClientId 当前授权确认的客户端id
   * @param principalName 当前授权确认用户的 username
   * @return 影响行数
   */
  int deleteByRegisteredClientIdAndPrincipalName(
      @Param("registeredClientId") String registeredClientId,
      @Param("principalName") String principalName);

  /**
   * 通过registeredClientId和principalName查询用户授权确认表
   *
   * @param registeredClientId 当前授权确认的客户端id
   * @param principalName 当前授权确认用户的 username
   * @return 可选的用户授权确认表数据
   */
  Optional<Oauth2AuthorizationConsent> selectByRegisteredClientIdAndPrincipalName(
      @Param("registeredClientId") String registeredClientId,
      @Param("principalName") String principalName);
}
