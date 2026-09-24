/**
 * Spring Security 适配层。BearerAuthenticationFilter 解析请求凭据并查询持久化会话， 已验证身份进入
 * SecurityContext；账号及设备编号从服务端取得，不采信请求体。 密码登录使用只读认证视图和 bcrypt，业务令牌轮换及撤销由应用服务控制事务。
 */
package io.github.lishangbu.avalon.identity.security;
