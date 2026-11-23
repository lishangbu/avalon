package io.github.lishangbu.avalon.chat.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.DatabaseForeignKey;
import io.github.lishangbu.avalon.platform.persistence.PartialIndex;
import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.Immutable;

/** JDBC 存储的 Hibernate 结构映射；业务读写由所属模块的存储类负责。 */
@DatabaseForeignKey(name = "chat_message_recipient_fk", columns = { "recipient" }, referencedTable = "player_character",
		referencedColumns = { "id" })
@DatabaseForeignKey(name = "chat_message_sender_fk", columns = { "sender" }, referencedTable = "player_character",
		referencedColumns = { "id" })
@PartialIndex(name = "chat_player_request", columns = { "sender", "request_id" }, predicate = "(sender IS NOT NULL)",
		unique = true)
@PartialIndex(name = "chat_public_history", columns = { "channel", "id DESC" }, predicate = "(recipient IS NULL)",
		unique = false)
@PartialIndex(name = "chat_retention", columns = { "created_at" },
		predicate = "(((channel)::text = 'WORLD'::text) OR (((channel)::text = 'SYSTEM'::text) AND (recipient IS NULL)))",
		unique = false)
@PartialIndex(name = "chat_sender_rate", columns = { "sender", "created_at DESC" }, predicate = "(sender IS NOT NULL)",
		unique = false)
@PartialIndex(name = "chat_system_request", columns = { "request_id" }, predicate = "(sender IS NULL)", unique = true)
@Entity
@Table(name = "chat_message", comment = "聊天纯文本主表；玩家消息与可信系统通知共享持久化，私聊和个人通知长期保留，公共历史七天到期。",
		indexes = { @Index(name = "chat_incoming", columnList = "recipient, id DESC"),
				@Index(name = "chat_outgoing", columnList = "sender, id DESC") },
		check = {
				@CheckConstraint(name = "chat_message_length",
						constraint = "((char_length(text) >= 1) AND (char_length(text) <= 500))"),
				@CheckConstraint(name = "chat_message_shape",
						constraint = "((((channel)::text = 'WORLD'::text) AND (sender IS NOT NULL) AND (recipient IS NULL)) OR (((channel)::text = 'PRIVATE'::text) AND (sender IS NOT NULL) AND (recipient IS NOT NULL) AND (sender <> recipient)) OR (((channel)::text = 'SYSTEM'::text) AND (sender IS NULL)))") })
@Immutable
public class ChatMessageRow {

	@Id
	@Column(name = "id", nullable = false, comment = "消息雪花主键；排序分页用字符串传输，不依赖主键表达提交先后")
	private Long id;

	@Column(name = "channel", nullable = false, length = 10, comment = "WORLD、PRIVATE或SYSTEM")
	private String channel;

	@Column(name = "sender", comment = "玩家消息的发送角色；系统消息为空")
	private Long sender;

	@Column(name = "sender_name", nullable = false, length = 32, comment = "发送时的角色显示名，系统显示系统")
	private String senderName;

	@Column(name = "recipient", comment = "私聊或个人通知的接收角色；公开消息为空")
	private Long recipient;

	@Column(name = "recipient_name", length = 32, comment = "私聊收件角色公开名快照")
	private String recipientName;

	@Column(name = "text", nullable = false, columnDefinition = "text", comment = "仅保存纯文本，页面禁止作为HTML渲染")
	private String text;

	@Column(name = "created_at", nullable = false, comment = "数据库插入时刻；公共消息七天到期")
	private Instant createdAt;

	@Column(name = "request_id", nullable = false, length = 100, comment = "玩家UUID或可信业务来源键，用于防重")
	private String requestId;

}
