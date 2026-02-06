package io.github.lishangbu.avalon.authorization.service.impl;

import io.github.lishangbu.avalon.authorization.entity.OauthRegisteredClient;
import io.github.lishangbu.avalon.authorization.repository.Oauth2RegisteredClientRepository;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/// JPA 注册客户端
///
/// @author lishangbu
/// @since 2025/8/17
@Component
@RequiredArgsConstructor
public class DefaultRegisteredClientRepository implements RegisteredClientRepository {
  private final Oauth2RegisteredClientRepository oauth2RegisteredClientRepository;

  /// 解析授权类型
  ///
  /// @param authorizationGrantType 授权类型字符串
  /// @return 授权类型对象
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
    }
    return new AuthorizationGrantType(authorizationGrantType); // Custom authorization grant type
  }

  /// 解析客户端认证方法
  ///
  /// @param clientAuthenticationMethod 认证方法字符串
  /// @return 认证方法对象
  private static ClientAuthenticationMethod resolveClientAuthenticationMethod(
      String clientAuthenticationMethod) {
    if (ClientAuthenticationMethod.CLIENT_SECRET_BASIC
        .getValue()
        .equals(clientAuthenticationMethod)) {
      return ClientAuthenticationMethod.CLIENT_SECRET_BASIC;
    } else if (ClientAuthenticationMethod.CLIENT_SECRET_POST
        .getValue()
        .equals(clientAuthenticationMethod)) {
      return ClientAuthenticationMethod.CLIENT_SECRET_POST;
    } else if (ClientAuthenticationMethod.NONE.getValue().equals(clientAuthenticationMethod)) {
      return ClientAuthenticationMethod.NONE;
    }
    return new ClientAuthenticationMethod(
        clientAuthenticationMethod); // Custom client authentication method
  }

  /// 保存注册客户端
  ///
  /// @param registeredClient 注册客户端
  @Override
  public void save(RegisteredClient registeredClient) {
    Assert.notNull(registeredClient, "registeredClient cannot be null");
    this.oauth2RegisteredClientRepository.save(toEntity(registeredClient));
  }

  /// 根据ID查找注册客户端
  ///
  /// @param id 客户端ID
  /// @return 注册客户端，未找到返回null
  @Override
  public RegisteredClient findById(String id) {
    Assert.hasText(id, "id cannot be empty");
    return this.oauth2RegisteredClientRepository.findById(id).map(this::toObject).orElse(null);
  }

  /// 根据客户端ID查找注册客户端
  ///
  /// @param clientId 客户端ID
  /// @return 注册客户端，未找到返回null
  @Override
  public RegisteredClient findByClientId(String clientId) {
    Assert.hasText(clientId, "clientId cannot be empty");
    return this.oauth2RegisteredClientRepository
        .findByClientId(clientId)
        .map(this::toObject)
        .orElse(null);
  }

  /// 将实体转换为对象
  ///
  /// @param client 实体
  /// @return 对象
  private RegisteredClient toObject(OauthRegisteredClient client) {
    Set<String> clientAuthenticationMethods =
        StringUtils.commaDelimitedListToSet(client.getClientAuthenticationMethods());
    Set<String> authorizationGrantTypes =
        StringUtils.commaDelimitedListToSet(client.getAuthorizationGrantTypes());
    Set<String> redirectUris = StringUtils.commaDelimitedListToSet(client.getRedirectUris());
    Set<String> postLogoutRedirectUris =
        StringUtils.commaDelimitedListToSet(client.getPostLogoutRedirectUris());
    Set<String> clientScopes = StringUtils.commaDelimitedListToSet(client.getScopes());

    RegisteredClient.Builder builder =
        RegisteredClient.withId(client.getId())
            .clientId(client.getClientId())
            .clientIdIssuedAt(client.getClientIdIssuedAt())
            .clientSecret(client.getClientSecret())
            .clientSecretExpiresAt(client.getClientSecretExpiresAt())
            .clientName(client.getClientName())
            .clientAuthenticationMethods(
                authenticationMethods ->
                    clientAuthenticationMethods.forEach(
                        authenticationMethod ->
                            authenticationMethods.add(
                                resolveClientAuthenticationMethod(authenticationMethod))))
            .authorizationGrantTypes(
                (grantTypes) ->
                    authorizationGrantTypes.forEach(
                        grantType -> grantTypes.add(resolveAuthorizationGrantType(grantType))))
            .redirectUris((uris) -> uris.addAll(redirectUris))
            .postLogoutRedirectUris((uris) -> uris.addAll(postLogoutRedirectUris))
            .scopes((scopes) -> scopes.addAll(clientScopes));

    ClientSettings.Builder clientSettingsBuilder = ClientSettings.builder();
    if (client.getRequireProofKey() != null) {
      clientSettingsBuilder.requireProofKey(client.getRequireProofKey());
    }
    if (client.getRequireAuthorizationConsent() != null) {
      clientSettingsBuilder.requireAuthorizationConsent(client.getRequireAuthorizationConsent());
    }
    if (client.getJwkSetUrl() != null) {
      clientSettingsBuilder.jwkSetUrl(client.getJwkSetUrl());
    }
    if (client.getTokenEndpointAuthenticationSigningAlgorithm() != null) {

      SignatureAlgorithm signatureAlgorithm =
          SignatureAlgorithm.from(client.getTokenEndpointAuthenticationSigningAlgorithm());
      if (signatureAlgorithm != null) {
        clientSettingsBuilder.tokenEndpointAuthenticationSigningAlgorithm(signatureAlgorithm);

      } else {
        MacAlgorithm macAlgorithm =
            MacAlgorithm.from(client.getTokenEndpointAuthenticationSigningAlgorithm());
        if (macAlgorithm != null) {
          clientSettingsBuilder.tokenEndpointAuthenticationSigningAlgorithm(macAlgorithm);
        }
      }
    }
    if (client.getX509CertificateSubjectDn() != null) {
      clientSettingsBuilder.x509CertificateSubjectDN(client.getX509CertificateSubjectDn());
    }
    builder.clientSettings(clientSettingsBuilder.build());

    TokenSettings.Builder tokenSettingsBuilder = TokenSettings.builder();
    tokenSettingsBuilder.reuseRefreshTokens(client.getReuseRefreshTokens());
    tokenSettingsBuilder.x509CertificateBoundAccessTokens(
        client.getX509CertificateBoundAccessTokens());
    if (client.getAuthorizationCodeTimeToLive() != null) {
      tokenSettingsBuilder.authorizationCodeTimeToLive(
          DurationStyle.detectAndParse(
              client.getAuthorizationCodeTimeToLive(), ChronoUnit.SECONDS));
    }
    if (client.getAccessTokenTimeToLive() != null) {
      tokenSettingsBuilder.accessTokenTimeToLive(
          DurationStyle.detectAndParse(client.getAccessTokenTimeToLive(), ChronoUnit.SECONDS));
    }
    if (client.getAccessTokenFormat() != null) {
      tokenSettingsBuilder.accessTokenFormat(new OAuth2TokenFormat(client.getAccessTokenFormat()));
    }
    if (client.getDeviceCodeTimeToLive() != null) {
      tokenSettingsBuilder.deviceCodeTimeToLive(
          DurationStyle.detectAndParse(client.getDeviceCodeTimeToLive(), ChronoUnit.SECONDS));
    }
    if (client.getRefreshTokenTimeToLive() != null) {
      tokenSettingsBuilder.refreshTokenTimeToLive(
          DurationStyle.detectAndParse(client.getRefreshTokenTimeToLive(), ChronoUnit.SECONDS));
    }
    if (client.getIdTokenSignatureAlgorithm() != null) {
      tokenSettingsBuilder.idTokenSignatureAlgorithm(
          SignatureAlgorithm.from(client.getIdTokenSignatureAlgorithm()));
    }
    builder.tokenSettings(tokenSettingsBuilder.build());

    return builder.build();
  }

  /// 将对象转换为实体
  ///
  /// @param registeredClient 对象
  /// @return 实体
  private OauthRegisteredClient toEntity(RegisteredClient registeredClient) {
    List<String> clientAuthenticationMethods =
        new ArrayList<>(registeredClient.getClientAuthenticationMethods().size());
    registeredClient
        .getClientAuthenticationMethods()
        .forEach(
            clientAuthenticationMethod ->
                clientAuthenticationMethods.add(clientAuthenticationMethod.getValue()));

    List<String> authorizationGrantTypes =
        new ArrayList<>(registeredClient.getAuthorizationGrantTypes().size());
    registeredClient
        .getAuthorizationGrantTypes()
        .forEach(
            authorizationGrantType ->
                authorizationGrantTypes.add(authorizationGrantType.getValue()));

    OauthRegisteredClient entity = new OauthRegisteredClient();
    entity.setId(registeredClient.getId());
    entity.setClientId(registeredClient.getClientId());
    entity.setClientIdIssuedAt(registeredClient.getClientIdIssuedAt());
    entity.setClientSecret(registeredClient.getClientSecret());
    entity.setClientSecretExpiresAt(registeredClient.getClientSecretExpiresAt());
    entity.setClientName(registeredClient.getClientName());
    entity.setClientAuthenticationMethods(
        StringUtils.collectionToCommaDelimitedString(clientAuthenticationMethods));
    entity.setAuthorizationGrantTypes(
        StringUtils.collectionToCommaDelimitedString(authorizationGrantTypes));
    entity.setRedirectUris(
        StringUtils.collectionToCommaDelimitedString(registeredClient.getRedirectUris()));
    entity.setPostLogoutRedirectUris(
        StringUtils.collectionToCommaDelimitedString(registeredClient.getPostLogoutRedirectUris()));
    entity.setScopes(StringUtils.collectionToCommaDelimitedString(registeredClient.getScopes()));
    ClientSettings registeredClientSettings = registeredClient.getClientSettings();
    if (registeredClientSettings != null) {
      entity.setRequireProofKey(registeredClientSettings.isRequireProofKey());
      entity.setRequireAuthorizationConsent(
          registeredClientSettings.isRequireAuthorizationConsent());
      entity.setTokenEndpointAuthenticationSigningAlgorithm(
          registeredClientSettings.getTokenEndpointAuthenticationSigningAlgorithm().getName());
      entity.setJwkSetUrl(registeredClientSettings.getJwkSetUrl());
      entity.setX509CertificateSubjectDn(registeredClientSettings.getX509CertificateSubjectDN());
    }

    TokenSettings registeredClientTokenSettings = registeredClient.getTokenSettings();
    if (registeredClientTokenSettings != null) {
      entity.setReuseRefreshTokens(registeredClientTokenSettings.isReuseRefreshTokens());
      entity.setX509CertificateBoundAccessTokens(
          registeredClientTokenSettings.isX509CertificateBoundAccessTokens());
      if (registeredClientTokenSettings.getAuthorizationCodeTimeToLive() != null) {
        entity.setAuthorizationCodeTimeToLive(
            registeredClientTokenSettings.getAuthorizationCodeTimeToLive().toString());
      }
      if (registeredClientTokenSettings.getAccessTokenTimeToLive() != null) {
        entity.setAccessTokenTimeToLive(
            registeredClientTokenSettings.getAccessTokenTimeToLive().toString());
      }
      if (registeredClientTokenSettings.getAccessTokenFormat() != null) {
        entity.setAccessTokenFormat(
            registeredClientTokenSettings.getAccessTokenFormat().getValue());
      }
      if (registeredClientTokenSettings.getDeviceCodeTimeToLive() != null) {
        entity.setDeviceCodeTimeToLive(
            registeredClientTokenSettings.getDeviceCodeTimeToLive().toString());
      }
      if (registeredClientTokenSettings.getRefreshTokenTimeToLive() != null) {
        entity.setRefreshTokenTimeToLive(
            registeredClientTokenSettings.getRefreshTokenTimeToLive().toString());
      }
      if (registeredClientTokenSettings.getIdTokenSignatureAlgorithm() != null) {
        entity.setIdTokenSignatureAlgorithm(
            registeredClientTokenSettings.getIdTokenSignatureAlgorithm().getName());
      }
    }
    return entity;
  }
}
