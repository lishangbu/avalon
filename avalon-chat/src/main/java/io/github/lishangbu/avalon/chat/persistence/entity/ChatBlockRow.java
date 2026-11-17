package io.github.lishangbu.avalon.chat.persistence.entity;

import org.hibernate.annotations.Immutable;
import io.github.lishangbu.avalon.platform.persistence.DatabaseForeignKey;
import jakarta.persistence.*;

/** JDBC 存储的 Hibernate 结构映射；业务读写由所属模块的存储类负责。 */
@DatabaseForeignKey(name = "chat_block_character_fk", columns = { "character_id" },
		referencedTable = "player_character", referencedColumns = { "id" })
@DatabaseForeignKey(name = "chat_block_peer_fk", columns = { "peer" }, referencedTable = "player_character",
		referencedColumns = { "id" })
@Entity
@Table(name = "chat_block", comment = "本人主动建立的有方向黑名单；阻止后续私聊并过滤本人所见发言，不删除对方角色或历史。",
		check = { @CheckConstraint(name = "chat_block_self", constraint = "(character_id <> peer)") })
@Immutable
@IdClass(ChatBlockRow.Key.class)
public class ChatBlockRow {

	@Id
	@Column(name = "character_id", nullable = false, comment = "执行拉黑的角色")
	private Long characterId;

	@Id
	@Column(name = "peer", nullable = false, comment = "被拉黑角色")
	private Long peer;

	@Column(name = "name", nullable = false, length = 32, comment = "公开显示名快照，用于解除拉黑")
	private String name;

	/** JDBC 行的复合主键。 */
	public record Key(Long characterId, Long peer) implements java.io.Serializable {
	}

}
