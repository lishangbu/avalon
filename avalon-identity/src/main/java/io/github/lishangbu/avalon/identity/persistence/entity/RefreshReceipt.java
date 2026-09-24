package io.github.lishangbu.avalon.identity.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.AssignedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * 已消费的刷新摘要用于检测重放；保留至原令牌到期，撤销所属会话而不返回新凭据。
 */
@Entity
@Table(name = "refresh_receipt")
@Getter
@Setter
public class RefreshReceipt extends AssignedEntity<String> {

	@Override
	public String getId() {
		return tokenHash;
	}

	/**
	 * 已消费刷新令牌的摘要主键。仅保留摘要，不保留任何可恢复的原令牌。
	 */
	@Id
	@Column(name = "token_hash")
	private String tokenHash;

	/**
	 * 对应设备会话主键，重放旧令牌时定位并撤销该设备。外键限制过早删除会话。
	 */
	@Column(name = "session_id", nullable = false)
	private Long sessionId;

	/**
	 * 重放检测记录的保留截止时间，与原设备会话绝对寿命保持一致。
	 */
	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

}
