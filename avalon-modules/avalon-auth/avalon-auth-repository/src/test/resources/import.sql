-- @formatter:off
insert into role (id,code) values (1,'ROLE_TEST');
-- 导入用户名为test,密码为123456的测试数据
insert into "user" (id, username, password) values (1, 'test', '{bcrypt}$2a$10$IlYJ6qn4gyXUL.CCLzlN4ujjzlfI.3UbB0VQrYSUmiaPKpcnxdU.G');
-- @formatter:on