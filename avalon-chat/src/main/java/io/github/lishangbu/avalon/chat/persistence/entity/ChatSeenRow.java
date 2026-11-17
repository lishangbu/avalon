package io.github.lishangbu.avalon.chat.persistence.entity;

import org.hibernate.annotations.Immutable;
import io.github.lishangbu.avalon.platform.persistence.DatabaseForeignKey;
import jakarta.persistence.*;

/** JDBC 存储的 Hibernate 结构映射；业务读写由所属模块的存储类负责。 */
@DatabaseForeignKey(name = "chat_seen_character_fk", columns = { "character_id" }, referencedTable = "player_character",
		referencedColumns = { "id" })
@DatabaseForeignKey(name = "chat_seen_message_fk", columns = { "message_id" }, referencedTable = "chat_message",
		referencedColumns = { "id" }, options = "ON DELETE CASCADE")
@Entity
@Table(name = "chat_seen", comment = "角色逐消息已读凭证；仅显式展示确认后写入，避免事务提交乱序造成新消息误读。")
@Immutable
@IdClass(ChatSeenRow.Key.class)
public class ChatSeenRow {

	@Id
	@Column(name = "character_id", nullable = false, comment = "实际展示并确认消息的角色")
	private Long characterId;

	@Id
	@Column(name = "message_id", nullable = false, comment = "逐条消息已读凭证，避免雪花生成与事务提交乱序漏标")
	private Long messageId;

	@Column(name = "seen_at", nullable = false, comment = "首次阅读时刻；重复确认幂等")
	private java.time.Instant seenAt;

	/** JDBC 行的复合主键。 */
	public record Key(Long characterId, Long messageId) implements java.io.Serializable {
	}

}
