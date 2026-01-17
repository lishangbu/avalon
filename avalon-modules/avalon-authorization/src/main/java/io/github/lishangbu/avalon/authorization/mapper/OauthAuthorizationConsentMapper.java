package io.github.lishangbu.avalon.authorization.mapper;

import io.github.lishangbu.avalon.authorization.entity.OauthAuthorizationConsent;
import org.apache.ibatis.annotations.Param;

/// OauthAuthorizationConsent 具有复合主键，因此使用原生 MyBatis XML 映射实现
public interface OauthAuthorizationConsentMapper {

  /// 根据 registeredClientId 与 principalName 查询授权确认记录（XML 实现）
  OauthAuthorizationConsent selectByRegisteredClientIdAndPrincipalName(
      @Param("registeredClientId") String registeredClientId,
      @Param("principalName") String principalName);

  /// 根据 registeredClientId 与 principalName 删除授权确认记录（XML 实现）
  int deleteByRegisteredClientIdAndPrincipalName(
      @Param("registeredClientId") String registeredClientId,
      @Param("principalName") String principalName);

  /// 插入一条授权确认记录（XML 实现）
  ///
  /// @param consent 要插入的实体
  /// @return 影响的行数
  int insert(OauthAuthorizationConsent consent);

  /// 根据复合主键 (registeredClientId, principalName) 更新授权确认记录，仅更新 authorities 字段
  ///
  /// @param consent 要更新的实体，必须包含 registeredClientId 与 principalName
  /// @return 影响的行数
  int updateById(OauthAuthorizationConsent consent);
}
