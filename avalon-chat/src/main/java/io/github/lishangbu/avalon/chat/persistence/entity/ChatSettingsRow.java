package io.github.lishangbu.avalon.chat.persistence.entity;

import io.github.lishangbu.avalon.platform.persistence.DatabaseForeignKey;
import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Immutable;

/** JDBC 存储的 Hibernate 结构映射；业务读写由所属模块的存储类负责。 */
@DatabaseForeignKey(name = "chat_settings_character_fk", columns = { "character_id" },
		referencedTable = "player_character", referencedColumns = { "id" })
@Entity
@Table(name = "chat_settings", comment = "角色级聊天接收偏好与最近成功发送时刻；查询不创建默认行，设置变更不重置频率。")
@Immutable
public class ChatSettingsRow {

	@Id
	@Column(name = "character_id", nullable = false, comment = "角色级拒收设置；与角色锁内发送状态一起持久化")
	private Long characterId;

	@ColumnDefault("false")
	@Column(name = "reject_strangers", nullable = false, comment = "仅接受已经成为好友的角色私聊")
	private Boolean rejectStrangers;

	@Column(name = "last_sent_at", comment = "跨频道最近一次成功发送时刻，失败发送不更新")
	private Instant lastSentAt;

}
