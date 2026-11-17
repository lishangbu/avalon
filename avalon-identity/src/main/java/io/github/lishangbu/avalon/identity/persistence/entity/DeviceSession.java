package io.github.lishangbu.avalon.identity.persistence.entity;

import org.hibernate.annotations.ColumnDefault;
import io.github.lishangbu.avalon.platform.persistence.DatabaseForeignKey;
import jakarta.persistence.Index;
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
import java.time.Instant;

/**
 * 每次登录独立创建设备会话。原始 Token 只在签发响应中出现，数据库仅保留摘要。
 */
@DatabaseForeignKey(name = "device_session_account_id_fkey", columns = { "account_id" }, referencedTable = "account",
		referencedColumns = { "id" })
@Entity
@Table(name = "device_session", comment = "每次登录独立创建设备会话。原始 Token 只在签发响应中出现，数据库仅保留摘要。",
		uniqueConstraints = {
				@UniqueConstraint(name = "device_session_access_hash_key", columnNames = { "access_hash" }),
				@UniqueConstraint(name = "device_session_refresh_hash_key", columnNames = { "refresh_hash" }) },
		indexes = { @Index(name = "idx_session_owner", columnList = "account_id") })
@Getter
@Setter
public class DeviceSession {

	/**
	 * CosId 分配的实例主键。Long 用于数据库和内部计算；若序列化则输出字符串，防止 JavaScript 整数精度丢失。
	 */
	@Id
	@CosIdGenerated
	@JsonSerialize(using = ToStringSerializer.class)
	@Column(name = "id", nullable = false, comment = "CosId 分配的实例主键。Long 用于数据库和内部计算；若序列化则输出字符串，防止 JavaScript 整数精度丢失。")
	private Long id;

	/**
	 * 不可变的账号归属。查询必须使用认证身份限定此字段，不能信任客户端提交的账号编号。
	 */
	@Column(name = "account_id", nullable = false, comment = "不可变的账号归属。查询必须使用认证身份限定此字段，不能信任客户端提交的账号编号。")
	private Long accountId;

	/**
	 * 设备显示名称，仅用于玩家辨认会话，不作为设备可信度或授权依据。
	 */
	@Column(name = "label", nullable = false, length = 80, comment = "设备显示名称，仅用于玩家辨认会话，不作为设备可信度或授权依据。")
	private String label;

	/**
	 * 当前访问令牌的 SHA-256 摘要；轮换会替换摘要，旧访问令牌随即不能匹配。
	 */
	@Column(name = "access_hash", nullable = false, length = 64, comment = "当前访问令牌的 SHA-256 摘要；轮换会替换摘要，旧访问令牌随即不能匹配。")
	private String accessHash;

	/**
	 * 当前刷新令牌摘要；被替换的摘要保存在刷新凭证表，用于检测严格重放。
	 */
	@Column(name = "refresh_hash", nullable = false, length = 64, comment = "当前刷新令牌摘要；被替换的摘要保存在刷新凭证表，用于检测严格重放。")
	private String refreshHash;

	/**
	 * 访问令牌截止时间，以服务端时钟判断；等于当前时间即视为过期。
	 */
	@Column(name = "access_until", nullable = false, comment = "访问令牌截止时间，以服务端时钟判断；等于当前时间即视为过期。")
	private Instant accessUntil;

	/**
	 * 设备会话的绝对截止时间，刷新不会无限延长；访问令牌也不得越过此截止时间使用。
	 */
	@Column(name = "refresh_until", nullable = false, comment = "设备会话的绝对截止时间，刷新不会无限延长；访问令牌也不得越过此截止时间使用。")
	private Instant refreshUntil;

	/**
	 * 显式撤销标记。严格刷新重放也会设置此标记，撤销必须在返回401前提交。
	 */
	@ColumnDefault("false")
	@Column(name = "revoked", nullable = false, comment = "显式撤销标记。严格刷新重放也会设置此标记，撤销必须在返回401前提交。")
	private boolean revoked;

}
