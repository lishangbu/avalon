INSERT INTO role (id, code, name, enabled)
VALUES (1, 'ROLE_TEST', '测试员', true),
       (2, 'ROLE_SUPER_ADMIN', '超级管理员', true);

INSERT INTO "user" (id, username, password)
VALUES (1, 'test', '{bcrypt}$2a$10$IlYJ6qn4gyXUL.CCLzlN4ujjzlfI.3UbB0VQrYSUmiaPKpcnxdU.G');

INSERT INTO user_role_relation (user_id, role_id)
VALUES (1, 1),
       (1, 2);

INSERT INTO menu (id, parent_id, disabled, extra, icon, key, label, show, path, name,
                             redirect, component, sort_order)
VALUES (1, NULL, false, NULL, 'iconify-[ph--info]', 'about', '关于项目', true, '/about', 'about', NULL, 'about/index', -999);

INSERT INTO role_menu_relation (role_id,menu_id)
VALUES (1, 1),
       (2, 1);