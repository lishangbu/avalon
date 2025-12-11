package io.github.lishangbu.avalon.authorization.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.lishangbu.avalon.authorization.entity.OauthAuthorization;

/** OauthAuthorization 的 MyBatis-Plus Mapper */
public interface OauthAuthorizationMapper extends BaseMapper<OauthAuthorization> {
  OauthAuthorization selectByState(String state);

  OauthAuthorization selectByAuthorizationCodeValue(String authorizationCode);

  OauthAuthorization selectByAccessTokenValue(String accessToken);

  OauthAuthorization selectByRefreshTokenValue(String refreshToken);

  OauthAuthorization selectByOidcIdTokenValue(String idToken);

  OauthAuthorization selectByUserCodeValue(String userCode);

  OauthAuthorization selectByDeviceCodeValue(String deviceCode);

  OauthAuthorization selectByToken(String token);
}
