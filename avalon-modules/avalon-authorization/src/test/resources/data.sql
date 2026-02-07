-- ----------------------------
-- Records of menu
-- ----------------------------
BEGIN;
INSERT INTO "menu" ("id", "parent_id", "disabled", "extra", "icon", "key", "label", "show", "path", "name", "redirect",
                    "component", "sort_order", "pinned", "show_tab", "enable_multi_tab")
VALUES (1, NULL, false, null, 'icon-[mage--dashboard-chart]', 'dashboard', '仪表板', true, 'dashboard', 'dashboard', '',
        'dashboard/index', 0, true, true, false),
       (2, NULL, false, null, 'icon-[ic--outline-dataset]', 'dataset', '数据集', true, 'dataset', 'dataset', '',
        'dataset',
        0, false, true, false),
       (3, 2, false, null, 'icon-[game-icons--barbed-star]', 'type', '属性管理', true, 'type', 'type', '',
        'dataset/type/index', 0, false, true, false),
       (4, 2, false, null, 'icon-[game-icons--beveled-star]', 'type-damage-relation', '属性克制管理', true,
        'type-damage-relation', 'type-damage-relation', '', 'dataset/type-damage-relation/index', 0, false, true,
        false),
       (5, 2, false, null, 'icon-[game-icons--diamond-hard]', 'berry-firmness', '树果硬度管理', true, 'berry-firmness',
        'berry-firmness', '', 'dataset/berry-firmness/index', 0, false, true, false),
       (6, 2, false, null, 'icon-[game-icons--opened-food-can]', 'berry-flavor', '树果风味管理', true, 'berry-flavor',
        'berry-flavor', '', 'dataset/berry-flavor/index', 0, false, true, false),
       (7, 2, false, null, 'icon-[game-icons--elderberry]', 'berry', '树果管理', true, 'berry', 'berry', '',
        'dataset/berry/index', 0, false, true, false);
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
VALUES (1, 1),
       (1, 2),
       (1, 3),
       (1, 4),
       (1, 5),
       (1, 6),
       (1, 7);
COMMIT;

-- ----------------------------
-- Records of user
-- ----------------------------
BEGIN;
INSERT INTO "user" ("id", "username", "password")
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

