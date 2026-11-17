package io.github.lishangbu.avalon.identity.persistence.entity;

import org.hibernate.annotations.ColumnDefault;
import jakarta.persistence.UniqueConstraint;
import io.github.lishangbu.avalon.platform.persistence.CosIdGenerated;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 账号写模型。仅保存密码哈希，禁止直接作为 HTTP 响应。
 */
@Entity
@Table(name = "account", comment = "账号写模型。仅保存密码哈希，禁止直接作为 HTTP 响应。",
		uniqueConstraints = { @UniqueConstraint(name = "account_username_key", columnNames = { "username" }) })
@Getter
@Setter
public class Account {

	/**
	 * CosId 分配的实例主键。Long 用于数据库和内部计算；若序列化则输出字符串，防止 JavaScript 整数精度丢失。
	 */
	@Id
	@CosIdGenerated
	@JsonSerialize(using = ToStringSerializer.class)
	@Column(name = "id", nullable = false, comment = "CosId 分配的实例主键。Long 用于数据库和内部计算；若序列化则输出字符串，防止 JavaScript 整数精度丢失。")
	private Long id;

	/**
	 * 登录账号，数据库唯一约束处理并发注册竞争。密码认证和角色名是不同概念。
	 */
	@Column(name = "username", nullable = false, length = 32, comment = "登录账号，数据库唯一约束处理并发注册竞争。密码认证和角色名是不同概念。")
	private String username;

	/**
	 * bcrypt 密码摘要；不保存明文密码，不允许返回给 HTTP 客户端或写入业务日志。
	 */
	@Column(name = "password_hash", nullable = false, length = 255,
			comment = "bcrypt 密码摘要；不保存明文密码，不允许返回给 HTTP 客户端或写入业务日志。")
	private String passwordHash;

	/**
	 * 逗号分隔的权限角色集合，供 Spring Security 和会话认证构建身份使用。
	 */
	@Column(name = "roles", nullable = false, length = 255, comment = "逗号分隔的权限角色集合，供 Security JPA 和会话认证构建身份使用。")
	private String roles;

	/** 三个自选导航标识按顺序保存；不存 URL，不能借配置注入任意跳转地址。 */
	@ColumnDefault("'COMPANIONS,INVENTORY,CHARACTER'::character varying")
	@Column(name = "navigation", nullable = false, length = 128, comment = "三个自选导航标识按顺序保存；不存 URL，不能借配置注入任意跳转地址。")
	private String navigation = "COMPANIONS,INVENTORY,CHARACTER";

}
