-- IAM 基础用户、角色、菜单和权限来自 avalon-spring 的 0.0.1 初始化数据，仅保留当前后端已落地能力。
INSERT INTO iam.role (code, name, enabled)
VALUES ('ROLE_SUPER_ADMIN', '超级管理员', TRUE),
       ('ROLE_TEST', '测试员', TRUE);

INSERT INTO iam.user_account (
    username,
    phone,
    email,
    avatar,
    enabled,
    password_hash,
    username_normalized,
    email_normalized,
    phone_normalized,
    password_updated_at
)
VALUES (
    'admin',
    '13800000000',
    'admin@example.com',
    NULL,
    TRUE,
    '$2a$10$IlYJ6qn4gyXUL.CCLzlN4ujjzlfI.3UbB0VQrYSUmiaPKpcnxdU.G',
    'admin',
    'admin@example.com',
    '13800000000',
    CURRENT_TIMESTAMP
);

INSERT INTO iam.user_role (user_id, role_id)
SELECT user_account.id, role.id
FROM (VALUES ('admin', 'ROLE_SUPER_ADMIN'), ('admin', 'ROLE_TEST')) AS binding(username, role_code)
JOIN iam.user_account ON iam.user_account.username = binding.username
JOIN iam.role ON iam.role.code = binding.role_code;

INSERT INTO iam.menu (
    disabled,
    extra,
    icon,
    menu_key,
    title,
    visible,
    path,
    route_name,
    redirect,
    component,
    sorting_order,
    pinned,
    show_tab,
    enable_multi_tab,
    menu_type,
    hidden,
    hide_children_in_menu,
    flat_menu,
    active_menu,
    external,
    target
)
VALUES (FALSE, NULL, 'mage:dashboard-chart', 'dashboard', '仪表板', TRUE, '/dashboard', 'dashboard', NULL,
        'dashboard/index', 0, TRUE, TRUE, FALSE, 'menu', FALSE, FALSE, FALSE, NULL, FALSE, NULL),
       (FALSE, NULL, 'ph:diamonds-four', 'system', '系统管理', TRUE, '/system', 'system', NULL, 'system', 0, FALSE,
        TRUE, FALSE, 'directory', FALSE, FALSE, FALSE, NULL, FALSE, NULL),
       (FALSE, NULL, 'ic:outline-dataset', 'dataset', '数据集', TRUE, '/dataset', 'dataset', NULL, 'dataset', 0,
        FALSE, TRUE, FALSE, 'directory', FALSE, FALSE, FALSE, NULL, FALSE, NULL);

INSERT INTO iam.menu (
    parent_id,
    disabled,
    extra,
    icon,
    menu_key,
    title,
    visible,
    path,
    route_name,
    redirect,
    component,
    sorting_order,
    pinned,
    show_tab,
    enable_multi_tab,
    menu_type,
    hidden,
    hide_children_in_menu,
    flat_menu,
    active_menu,
    external,
    target
)
SELECT parent.id,
       menu.disabled,
       menu.extra,
       menu.icon,
       menu.menu_key,
       menu.title,
       menu.visible,
       menu.path,
       menu.route_name,
       menu.redirect,
       menu.component,
       menu.sorting_order,
       menu.pinned,
       menu.show_tab,
       menu.enable_multi_tab,
       menu.menu_type,
       menu.hidden,
       menu.hide_children_in_menu,
       menu.flat_menu,
       menu.active_menu,
       menu.external,
       menu.target
FROM (
    VALUES ('system', FALSE, NULL, 'ph:user-list', 'user', '用户管理', TRUE, '/system/user', 'user', NULL,
            'system/user/index', 0, FALSE, TRUE, FALSE, 'menu', FALSE, FALSE, FALSE, NULL, FALSE, NULL),
           ('system', FALSE, NULL, 'ph:person-light', 'role', '角色管理', TRUE, '/system/role', 'role', NULL,
            'system/role/index', 0, FALSE, TRUE, FALSE, 'menu', FALSE, FALSE, FALSE, NULL, FALSE, NULL),
           ('system', FALSE, NULL, 'ph:function-bold', 'menu', '菜单管理', TRUE, '/system/menu', 'menu', NULL,
            'system/menu/index', 0, FALSE, TRUE, FALSE, 'menu', FALSE, FALSE, FALSE, NULL, FALSE, NULL),
           ('system', FALSE, NULL, 'ph:key-bold', 'permission', '权限管理', TRUE, '/system/permission', 'permission',
            NULL, 'system/permission/index', 0, FALSE, TRUE, FALSE, 'menu', FALSE, FALSE, FALSE, NULL, FALSE, NULL),
           ('dataset', FALSE, NULL, 'game-icons:barbed-star', 'type', '属性管理', TRUE, '/dataset/type', 'type', NULL,
            'dataset/type/index', 0, FALSE, TRUE, FALSE, 'menu', FALSE, FALSE, FALSE, NULL, FALSE, NULL),
           ('dataset', FALSE, NULL, 'game-icons:beveled-star', 'type-effectiveness', '属性克制管理', TRUE,
            '/dataset/type-effectiveness', 'type-effectiveness', NULL, 'dataset/type-effectiveness/index', 0, FALSE,
            TRUE, FALSE, 'menu', FALSE, FALSE, FALSE, NULL, FALSE, NULL),
           ('dataset', FALSE, NULL, 'game-icons:aura', 'ability', '特性管理', TRUE, '/dataset/ability', 'ability', NULL,
            'dataset/ability/index', 0, FALSE, TRUE, FALSE, 'menu', FALSE, FALSE, FALSE, NULL, FALSE, NULL),
           ('dataset', FALSE, NULL, 'game-icons:growth', 'growth-rate', '成长速率管理', TRUE, '/dataset/growth-rate',
            'growth-rate', NULL, 'dataset/growth-rate/index', 0, FALSE, TRUE, FALSE, 'menu', FALSE, FALSE, FALSE,
            NULL, FALSE, NULL),
           ('dataset', FALSE, NULL, 'game-icons:yin-yang', 'nature', '性格管理', TRUE, '/dataset/nature', 'nature', NULL,
            'dataset/nature/index', 0, FALSE, TRUE, FALSE, 'menu', FALSE, FALSE, FALSE, NULL, FALSE, NULL),
           ('dataset', FALSE, NULL, 'ph:first-aid-kit-bold', 'move-ailment', '招式异常管理', TRUE,
            '/dataset/move-ailment', 'move-ailment', NULL, 'dataset/move-ailment/index', 0, FALSE, TRUE, FALSE,
            'menu', FALSE, FALSE, FALSE, NULL, FALSE, NULL),
           ('dataset', FALSE, NULL, 'ph:squares-four-bold', 'move-category', '招式类别管理', TRUE,
            '/dataset/move-category', 'move-category', NULL, 'dataset/move-category/index', 0, FALSE, TRUE, FALSE,
            'menu', FALSE, FALSE, FALSE, NULL, FALSE, NULL),
           ('dataset', FALSE, NULL, 'ph:graduation-cap-bold', 'move-learn-method', '招式学习方式管理', TRUE,
            '/dataset/move-learn-method', 'move-learn-method', NULL, 'dataset/move-learn-method/index', 0, FALSE,
            TRUE, FALSE, 'menu', FALSE, FALSE, FALSE, NULL, FALSE, NULL),
           ('dataset', FALSE, NULL, 'ph:crosshair-bold', 'move-target', '招式目标管理', TRUE, '/dataset/move-target',
            'move-target', NULL, 'dataset/move-target/index', 0, FALSE, TRUE, FALSE, 'menu', FALSE, FALSE, FALSE,
            NULL, FALSE, NULL),
           ('dataset', FALSE, NULL, 'ph:cube-bold', 'item', '道具管理', TRUE, '/dataset/item', 'item', NULL,
            'dataset/item/index', 0, FALSE, TRUE, FALSE, 'menu', FALSE, FALSE, FALSE, NULL, FALSE, NULL),
           ('dataset', FALSE, NULL, 'ph:sword-bold', 'move', '招式管理', TRUE, '/dataset/move', 'move', NULL,
            'dataset/move/index', 0, FALSE, TRUE, FALSE, 'menu', FALSE, FALSE, FALSE, NULL, FALSE, NULL),
           ('dataset', FALSE, NULL, 'ph:dna-bold', 'creature-species', '精灵种族管理', TRUE, '/dataset/creature-species',
            'creature-species', NULL, 'dataset/creature-species/index', 0, FALSE, TRUE, FALSE, 'menu', FALSE, FALSE,
            FALSE, NULL, FALSE, NULL),
           ('dataset', FALSE, NULL, 'ph:git-merge-bold', 'creature-evolution', '进化条件管理', TRUE,
            '/dataset/creature-evolution', 'creature-evolution', NULL, 'dataset/creature-evolution/index', 0, FALSE,
            TRUE, FALSE, 'menu', FALSE, FALSE, FALSE, NULL, FALSE, NULL)
) AS menu(
    parent_menu_key,
    disabled,
    extra,
    icon,
    menu_key,
    title,
    visible,
    path,
    route_name,
    redirect,
    component,
    sorting_order,
    pinned,
    show_tab,
    enable_multi_tab,
    menu_type,
    hidden,
    hide_children_in_menu,
    flat_menu,
    active_menu,
    external,
    target
)
JOIN iam.menu parent ON parent.menu_key = menu.parent_menu_key;

INSERT INTO iam.permission (menu_id, code, name, enabled, sorting_order)
SELECT menu.id, permission.code, permission.name, permission.enabled, permission.sorting_order
FROM (
    VALUES ('user', 'system:user:query', '查看用户', TRUE, 0),
           ('user', 'system:user:create', '新增用户', TRUE, 1),
           ('user', 'system:user:update', '编辑用户', TRUE, 2),
           ('user', 'system:user:delete', '删除用户', TRUE, 3),
           ('role', 'system:role:query', '查看角色', TRUE, 0),
           ('role', 'system:role:create', '新增角色', TRUE, 1),
           ('role', 'system:role:update', '编辑角色', TRUE, 2),
           ('role', 'system:role:delete', '删除角色', TRUE, 3),
           ('menu', 'system:menu:query', '查看菜单', TRUE, 0),
           ('menu', 'system:menu:create', '新增菜单', TRUE, 1),
           ('menu', 'system:menu:update', '编辑菜单', TRUE, 2),
           ('menu', 'system:menu:delete', '删除菜单', TRUE, 3),
           ('permission', 'system:permission:query', '查看权限点', TRUE, 0),
           ('permission', 'system:permission:create', '新增权限点', TRUE, 1),
           ('permission', 'system:permission:update', '编辑权限点', TRUE, 2),
           ('permission', 'system:permission:delete', '删除权限点', TRUE, 3)
) AS permission(menu_key, code, name, enabled, sorting_order)
JOIN iam.menu menu ON menu.menu_key = permission.menu_key;

INSERT INTO iam.role_menu (role_id, menu_id)
SELECT role.id, menu.id
FROM (
    VALUES ('ROLE_SUPER_ADMIN', 'dashboard'),
           ('ROLE_SUPER_ADMIN', 'system'),
           ('ROLE_SUPER_ADMIN', 'user'),
           ('ROLE_SUPER_ADMIN', 'role'),
           ('ROLE_SUPER_ADMIN', 'menu'),
           ('ROLE_SUPER_ADMIN', 'permission'),
           ('ROLE_SUPER_ADMIN', 'dataset'),
           ('ROLE_SUPER_ADMIN', 'type'),
           ('ROLE_SUPER_ADMIN', 'type-effectiveness'),
           ('ROLE_SUPER_ADMIN', 'ability'),
           ('ROLE_SUPER_ADMIN', 'growth-rate'),
           ('ROLE_SUPER_ADMIN', 'nature'),
           ('ROLE_SUPER_ADMIN', 'move-ailment'),
           ('ROLE_SUPER_ADMIN', 'move-category'),
           ('ROLE_SUPER_ADMIN', 'move-learn-method'),
           ('ROLE_SUPER_ADMIN', 'move-target'),
           ('ROLE_SUPER_ADMIN', 'item'),
           ('ROLE_SUPER_ADMIN', 'move'),
           ('ROLE_SUPER_ADMIN', 'creature-species'),
           ('ROLE_SUPER_ADMIN', 'creature-evolution')
) AS binding(role_code, menu_key)
JOIN iam.role role ON role.code = binding.role_code
JOIN iam.menu menu ON menu.menu_key = binding.menu_key;

INSERT INTO iam.role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM (
    VALUES ('ROLE_SUPER_ADMIN', 'system:user:query'),
           ('ROLE_SUPER_ADMIN', 'system:user:create'),
           ('ROLE_SUPER_ADMIN', 'system:user:update'),
           ('ROLE_SUPER_ADMIN', 'system:user:delete'),
           ('ROLE_SUPER_ADMIN', 'system:role:query'),
           ('ROLE_SUPER_ADMIN', 'system:role:create'),
           ('ROLE_SUPER_ADMIN', 'system:role:update'),
           ('ROLE_SUPER_ADMIN', 'system:role:delete'),
           ('ROLE_SUPER_ADMIN', 'system:menu:query'),
           ('ROLE_SUPER_ADMIN', 'system:menu:create'),
           ('ROLE_SUPER_ADMIN', 'system:menu:update'),
           ('ROLE_SUPER_ADMIN', 'system:menu:delete'),
           ('ROLE_SUPER_ADMIN', 'system:permission:query'),
           ('ROLE_SUPER_ADMIN', 'system:permission:create'),
           ('ROLE_SUPER_ADMIN', 'system:permission:update'),
           ('ROLE_SUPER_ADMIN', 'system:permission:delete')
) AS binding(role_code, permission_code)
JOIN iam.role role ON role.code = binding.role_code
JOIN iam.permission permission ON permission.code = binding.permission_code;
