package io.github.lishangbu.avalon.authorization.service.impl;

import io.github.lishangbu.avalon.authorization.entity.OauthAuthorization;
import io.github.lishangbu.avalon.authorization.repository.Oauth2AuthorizationRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Implementations of this interface are responsible for the management of {@link
 * OAuth2Authorization OAuth 2.0 Authorization(s)}.
 *
 * @author lishangbu
 * @since 2025/8/17
 */
@Service
@RequiredArgsConstructor
public class DefaultOAuth2AuthorizationService implements OAuth2AuthorizationService {
  private final Oauth2AuthorizationRepository oauth2AuthorizationRepository;

  private static AuthorizationGrantType resolveAuthorizationGrantType(
      String authorizationGrantType) {
    if (AuthorizationGrantType.AUTHORIZATION_CODE.getValue().equals(authorizationGrantType)) {
      return AuthorizationGrantType.AUTHORIZATION_CODE;
    } else if (AuthorizationGrantType.CLIENT_CREDENTIALS
        .getValue()
        .equals(authorizationGrantType)) {
      return AuthorizationGrantType.CLIENT_CREDENTIALS;
    } else if (AuthorizationGrantType.REFRESH_TOKEN.getValue().equals(authorizationGrantType)) {
      return AuthorizationGrantType.REFRESH_TOKEN;
    } else if (AuthorizationGrantType.DEVICE_CODE.getValue().equals(authorizationGrantType)) {
      return AuthorizationGrantType.DEVICE_CODE;
    }
    return new AuthorizationGrantType(authorizationGrantType); // Custom authorization grant type
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void save(OAuth2Authorization authorization) {
    Assert.notNull(authorization, "authorization cannot be null");
    this.oauth2AuthorizationRepository.save(toEntity(authorization));
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void remove(OAuth2Authorization authorization) {
    Assert.notNull(authorization, "authorization cannot be null");
    this.oauth2AuthorizationRepository.deleteById(authorization.getId());
  }

  @Override
  public OAuth2Authorization findById(String id) {
    Assert.hasText(id, "id cannot be empty");
    return this.oauth2AuthorizationRepository
        .findById(id)
        .map(OauthAuthorization::getAuthorizationObject)
        .orElse(null);
  }

  @Override
  public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
    Assert.hasText(token, "token cannot be empty");

    Optional<OauthAuthorization> result;
    if (tokenType == null) {
      result =
          this.oauth2AuthorizationRepository
              .findByStateOrAuthorizationCodeValueOrAccessTokenValueOrRefreshTokenValueOrOidcIdTokenValueOrUserCodeValueOrDeviceCodeValue(
                  token);
    } else if (OAuth2ParameterNames.STATE.equals(tokenType.getValue())) {
      result = this.oauth2AuthorizationRepository.findByState(token);
    } else if (OAuth2ParameterNames.CODE.equals(tokenType.getValue())) {
      result = this.oauth2AuthorizationRepository.findByAuthorizationCodeValue(token);
    } else if (OAuth2ParameterNames.ACCESS_TOKEN.equals(tokenType.getValue())) {
      result = this.oauth2AuthorizationRepository.findByAccessTokenValue(token);
    } else if (OAuth2ParameterNames.REFRESH_TOKEN.equals(tokenType.getValue())) {
      result = this.oauth2AuthorizationRepository.findByRefreshTokenValue(token);
    } else if (OidcParameterNames.ID_TOKEN.equals(tokenType.getValue())) {
      result = this.oauth2AuthorizationRepository.findByOidcIdTokenValue(token);
    } else if (OAuth2ParameterNames.USER_CODE.equals(tokenType.getValue())) {
      result = this.oauth2AuthorizationRepository.findByUserCodeValue(token);
    } else if (OAuth2ParameterNames.DEVICE_CODE.equals(tokenType.getValue())) {
      result = this.oauth2AuthorizationRepository.findByDeviceCodeValue(token);
    } else {
      result = Optional.empty();
    }

    return result.map(OauthAuthorization::getAuthorizationObject).orElse(null);
  }

  private OauthAuthorization toEntity(OAuth2Authorization authorization) {
    OauthAuthorization entity = new OauthAuthorization();

    // 直接序列化整个OAuth2Authorization对象
    entity.setAuthorizationObject(authorization);

    // 设置查询字段
    entity.setId(authorization.getId());
    entity.setRegisteredClientId(authorization.getRegisteredClientId());
    entity.setPrincipalName(authorization.getPrincipalName());
    entity.setAuthorizationGrantType(authorization.getAuthorizationGrantType().getValue());
    entity.setAuthorizedScopes(
        StringUtils.collectionToDelimitedString(authorization.getAuthorizedScopes(), ","));
    entity.setState(authorization.getAttribute(OAuth2ParameterNames.STATE));

    // 设置token值字段用于查询
    OAuth2Authorization.Token<OAuth2AuthorizationCode> authorizationCode =
        authorization.getToken(OAuth2AuthorizationCode.class);
    if (authorizationCode != null) {
      OAuth2Token token = authorizationCode.getToken();
      entity.setAuthorizationCodeValue(token.getTokenValue());
      entity.setAuthorizationCodeIssuedAt(token.getIssuedAt());
      entity.setAuthorizationCodeExpiresAt(token.getExpiresAt());
      entity.setAuthorizationCodeMetadata(authorizationCode.getMetadata());
    }

    OAuth2Authorization.Token<OAuth2AccessToken> accessToken =
        authorization.getToken(OAuth2AccessToken.class);
    if (accessToken != null) {
      OAuth2Token token = accessToken.getToken();
      entity.setAccessTokenValue(token.getTokenValue());
      entity.setAccessTokenIssuedAt(token.getIssuedAt());
      entity.setAccessTokenExpiresAt(token.getExpiresAt());
      entity.setAccessTokenMetadata(accessToken.getMetadata());
      if (accessToken.getToken().getScopes() != null) {
        entity.setAccessTokenScopes(
            StringUtils.collectionToDelimitedString(accessToken.getToken().getScopes(), ","));
      }
    }

    OAuth2Authorization.Token<OAuth2RefreshToken> refreshToken =
        authorization.getToken(OAuth2RefreshToken.class);
    if (refreshToken != null) {
      OAuth2Token token = refreshToken.getToken();
      entity.setRefreshTokenValue(token.getTokenValue());
      entity.setRefreshTokenIssuedAt(token.getIssuedAt());
      entity.setRefreshTokenExpiresAt(token.getExpiresAt());
      entity.setRefreshTokenMetadata(refreshToken.getMetadata());
    }

    OAuth2Authorization.Token<OidcIdToken> oidcIdToken = authorization.getToken(OidcIdToken.class);
    if (oidcIdToken != null) {
      OAuth2Token token = oidcIdToken.getToken();
      entity.setOidcIdTokenValue(token.getTokenValue());
      entity.setOidcIdTokenIssuedAt(token.getIssuedAt());
      entity.setOidcIdTokenExpiresAt(token.getExpiresAt());
      entity.setOidcIdTokenMetadata(oidcIdToken.getMetadata());
      entity.setOidcIdTokenClaims(oidcIdToken.getClaims());
    }

    OAuth2Authorization.Token<OAuth2UserCode> userCode =
        authorization.getToken(OAuth2UserCode.class);
    if (userCode != null) {
      OAuth2Token token = userCode.getToken();
      entity.setUserCodeValue(token.getTokenValue());
      entity.setUserCodeIssuedAt(token.getIssuedAt());
      entity.setUserCodeExpiresAt(token.getExpiresAt());
      entity.setUserCodeMetadata(userCode.getMetadata());
    }

    OAuth2Authorization.Token<OAuth2DeviceCode> deviceCode =
        authorization.getToken(OAuth2DeviceCode.class);
    if (deviceCode != null) {
      OAuth2Token token = deviceCode.getToken();
      entity.setDeviceCodeValue(token.getTokenValue());
      entity.setDeviceCodeIssuedAt(token.getIssuedAt());
      entity.setDeviceCodeExpiresAt(token.getExpiresAt());
      entity.setDeviceCodeMetadata(deviceCode.getMetadata());
    }

    return entity;
  }
}
