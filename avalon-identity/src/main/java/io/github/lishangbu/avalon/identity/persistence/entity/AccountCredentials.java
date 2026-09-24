package io.github.lishangbu.avalon.identity.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;

import org.hibernate.annotations.Immutable;

/**
 * Spring Security 的只读凭据投影；认证与账号写事务均位于身份模块。 视图直接读取账号表，没有第二份密码数据，也不通过此实体更新账号。
 */
@Entity
@Table(name = "account_credentials")
@Immutable
@Getter
public class AccountCredentials {

	/**
	 * 只读视图映射的账号主键，与账号表相同，不重新生成；本实体不得作为HTTP响应序列化。
	 */
	@Id
	private Long id;

	/**
	 * 登录账号，数据库唯一约束处理并发注册竞争。密码认证和角色名是不同概念。
	 */
	private String username;

	/**
	 * bcrypt 密码摘要；不保存明文密码，不允许返回给 HTTP 客户端或写入业务日志。
	 */
	@Column(name = "password_hash")
	private String passwordHash;

	/**
	 * 逗号分隔的权限角色集合，供 Spring Security 和会话认证构建身份使用。
	 */
	private String roles;

}
