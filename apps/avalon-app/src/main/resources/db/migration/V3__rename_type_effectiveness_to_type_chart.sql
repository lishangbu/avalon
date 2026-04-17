UPDATE iam.menu
SET menu_key = 'type-chart',
    title = '属性矩阵管理',
    path = '/catalog/type-chart',
    route_name = 'type-chart',
    component = 'catalog/type-chart/index'
WHERE menu_key = 'type-effectiveness';

UPDATE iam.permission
SET code = 'catalog:type-chart:query',
    name = '查看属性矩阵'
WHERE code = 'catalog:type-effectiveness:query';

UPDATE iam.permission
SET code = 'catalog:type-chart:update',
    name = '编辑属性矩阵'
WHERE code = 'catalog:type-effectiveness:update';

DELETE
FROM iam.permission
WHERE code IN (
    'catalog:type-effectiveness:create',
    'catalog:type-effectiveness:delete'
);
