/*
 * Copyright 2004-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.lishangbu.avalon.oauth2.common.core.endpoint;

/**
 * Standard and custom (non-standard) parameter names defined in the OAuth Parameters Registry and
 * used by the authorization endpoint, token endpoint and token revocation endpoint.
 *
 * @author Joe Grandja
 * @author Steve Riesenberg
 * @see <a target="_blank" href="https://tools.ietf.org/html/rfc6749#section-11.2">11.2 OAuth
 *     Parameters Registry</a>
 * @since 5.0
 */
public final class OAuth2PasswordParameterNames {

  /** {@code username} - used in Access Token Request. */
  public static final String USERNAME = "username";

  /** {@code password} - used in Access Token Request. */
  public static final String PASSWORD = "password";

  private OAuth2PasswordParameterNames() {}
}
