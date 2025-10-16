-- 用户表数据
INSERT INTO "user" (id, username, password) VALUES (1, 'test', '{bcrypt}$2a$10$IlYJ6qn4gyXUL.CCLzlN4ujjzlfI.3UbB0VQrYSUmiaPKpcnxdU.G');

-- 角色表数据
INSERT INTO role (id, code, name, enabled) VALUES (1, 'ROLE_TEST', '测试员', true);
INSERT INTO role (id, code, name, enabled) VALUES (2, 'ROLE_SUPER_ADMIN', '超级管理员', true);

-- 用户角色关系表数据
INSERT INTO user_role_relation (user_id, role_id) VALUES (1, 1);
INSERT INTO user_role_relation (user_id, role_id) VALUES (1, 2);


-- 客户端注册表数据
INSERT INTO oauth_registered_client (
  id, client_id, client_id_issued_at, client_secret, client_secret_expires_at, client_name, client_authentication_methods, authorization_grant_types, redirect_uris, post_logout_redirect_uris, scopes, require_proof_key, require_authorization_consent, jwk_set_url, token_endpoint_authentication_signing_algorithm, x509_certificate_subject_dn, authorization_code_time_to_live, access_token_time_to_live, access_token_format, device_code_time_to_live, reuse_refresh_tokens, refresh_token_time_to_live, id_token_signature_algorithm, x509_certificate_bound_access_tokens
) VALUES (
  '1', 'client', '2025-08-13 00:11:22', '{noop}client', '5202-08-13 00:11:22', '测试客户端的客户端', 'client_secret_basic,client_secret_post,client_secret_jwt', 'refresh_token,client_credentials,password', NULL, 'http://localhost:8080', 'openid,profile', 'false', 'false', NULL, 'RS256', NULL, '2h', '2h', 'self-contained', '1h', 'true', '30d', 'RS256', 'false'
);