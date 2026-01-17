package io.github.lishangbu.avalon.authorization.controller;

import io.github.lishangbu.avalon.oauth2.common.userdetails.UserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/// 令牌接口
///
/// 提供登出与当前用户令牌信息查询接口
///
/// @author vains
@RequestMapping("/token")
@RestController
@RequiredArgsConstructor
public class TokenController {

  private final OAuth2AuthorizationService oAuth2AuthorizationService;

  @DeleteMapping("/logout")
  public void logout() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null
        && authentication.getCredentials() instanceof OAuth2AccessToken accessToken) {
      OAuth2Authorization auth2Authorization =
          oAuth2AuthorizationService.findByToken(
              accessToken.getTokenValue(), OAuth2TokenType.ACCESS_TOKEN);
      if (auth2Authorization != null) {
        oAuth2AuthorizationService.remove(auth2Authorization);
        SecurityContextHolder.clearContext();
      }
    }
  }

  @GetMapping("/info")
  public UserInfo user(@AuthenticationPrincipal UserInfo user) {
    return user;
  }
}
