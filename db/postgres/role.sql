BEGIN;
insert into public.role (id, code, enabled, name)
values (1, 'ROLE_SUPER_ADMIN', true, '超级管理员'),
       (2, 'ROLE_TEST', true, '测试员');
COMMIT;