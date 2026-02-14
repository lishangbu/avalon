-- ----------------------------
-- Records of menu
-- ----------------------------
BEGIN;
INSERT INTO "menu" ("id", "parent_id", "disabled", "extra", "icon", "key", "label", "show", "path", "name", "redirect",
                    "component", "sorting_order", "pinned", "show_tab", "enable_multi_tab")
VALUES (1, NULL, false, null, 'icon-[mage--dashboard-chart]', 'dashboard', '仪表板', true, 'dashboard', 'dashboard', '',
        'dashboard/index', 0, true, true, false);
COMMIT;

-- ----------------------------
-- Records of oauth_registered_client
-- ----------------------------
BEGIN;
INSERT INTO "oauth_registered_client" ("id", "client_id", "client_id_issued_at", "client_secret",
                                       "client_secret_expires_at", "client_name", "client_authentication_methods",
                                       "authorization_grant_types", "redirect_uris", "post_logout_redirect_uris",
                                       "scopes", "require_proof_key", "require_authorization_consent", "jwk_set_url",
                                       "token_endpoint_authentication_signing_algorithm", "x509_certificate_subject_dn",
                                       "authorization_code_time_to_live", "access_token_time_to_live",
                                       "access_token_format", "device_code_time_to_live", "reuse_refresh_tokens",
                                       "refresh_token_time_to_live", "id_token_signature_algorithm",
                                       "x509_certificate_bound_access_tokens")
VALUES ('1', 'client', '2025-08-12 16:11:22+00', '{noop}client', '5202-08-12 16:11:22+00', '测试客户端的客户端',
        'client_secret_basic,client_secret_post,client_secret_jwt', 'refresh_token,client_credentials,password', '',
        'http://localhost:8080', 'openid,profile', false, false, '', 'RS256', '', '2h', '2h', 'self-contained', '1h',
        true, '30d', 'RS256', false),
       ('2', 'test', '2025-08-12 16:11:22+00', '{noop}test', '5202-08-12 16:11:22+00', '测试REFERENCE模式的客户端',
        'client_secret_basic,client_secret_post,client_secret_jwt', 'refresh_token,client_credentials,password', '',
        'http://localhost:8080', 'openid,profile', false, false, '', 'RS256', '', '2h', '2h', 'reference', '1h', true,
        '30d', 'RS256', false);
COMMIT;

-- ----------------------------
-- Records of role
-- ----------------------------
BEGIN;
INSERT INTO "role" ("id", "code", "name", "enabled")
VALUES (1, 'ROLE_SUPER_ADMIN', '超级管理员', true),
       (2, 'ROLE_TEST', '测试员', true);
COMMIT;

-- ----------------------------
-- Records of role_menu_relation
-- ----------------------------
BEGIN;
INSERT INTO "role_menu_relation" ("role_id", "menu_id")
VALUES (1, 1);
COMMIT;

-- ----------------------------
-- Records of user
-- ----------------------------
BEGIN;
INSERT INTO "users" ("id", "username", "hashed_password")
VALUES (1, 'admin', '{bcrypt}$2a$10$IlYJ6qn4gyXUL.CCLzlN4ujjzlfI.3UbB0VQrYSUmiaPKpcnxdU.G');
COMMIT;

-- ----------------------------
-- Records of user_role_relation
-- ----------------------------
BEGIN;
INSERT INTO "user_role_relation" ("user_id", "role_id")
VALUES (1, 1),
       (1, 2);
COMMIT;

