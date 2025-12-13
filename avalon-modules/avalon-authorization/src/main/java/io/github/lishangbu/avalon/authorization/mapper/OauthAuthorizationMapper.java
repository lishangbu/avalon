package io.github.lishangbu.avalon.authorization.mapper;

import io.github.lishangbu.avalon.authorization.entity.OauthAuthorization;
import java.util.Optional;

/**
 * OauthAuthorization 数据访问接口
 *
 * <p>提供对 oauth_authorization 表的插入与按 id 更新操作 所有 jsonb 字段在 SQL 中使用 PostgreSQL 的 ::jsonb 语法进行参数绑定
 */
public interface OauthAuthorizationMapper {
  OauthAuthorization selectByState(String state);

  OauthAuthorization selectByAuthorizationCodeValue(String authorizationCode);

  OauthAuthorization selectByAccessTokenValue(String accessToken);

  OauthAuthorization selectByRefreshTokenValue(String refreshToken);

  OauthAuthorization selectByOidcIdTokenValue(String idToken);

  OauthAuthorization selectByUserCodeValue(String userCode);

  OauthAuthorization selectByDeviceCodeValue(String deviceCode);

  OauthAuthorization selectByToken(String token);

  /**
   * 根据 id 查询授权记录
   *
   * @param id 授权记录 id
   * @return 对应的授权实体，找不到时返回 null
   */
  Optional<OauthAuthorization> selectById(String id);

  /**
   * 根据 id 删除授权记录
   *
   * @param id 授权记录 id
   * @return 受影响的行数
   */
  int deleteById(String id);

  /**
   * 插入一条完整的授权信息记录
   *
   * @param entity 要插入的实体，必须包含 id 字段
   * @return 受影响的行数
   */
  int insert(OauthAuthorization entity);

  /**
   * 根据 id 更新整条记录
   *
   * @param entity 包含 id 与要更新字段值的实体
   * @return 受影响的行数
   */
  int updateById(OauthAuthorization entity);
}
