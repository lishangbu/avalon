package io.github.lishangbu.avalon.world.persistence.entity;

import org.hibernate.annotations.ColumnDefault;
import io.github.lishangbu.avalon.platform.persistence.DatabaseForeignKey;
import io.github.lishangbu.avalon.content.quest.model.QuestType;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.platform.persistence.CosIdGenerated;
import jakarta.persistence.*;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 角色的地图指引偏好。两个固定槽位只控制显示，不参与目标累计与交付资格判定。 */
@DatabaseForeignKey(name = "fk_quest_tracking_character", columns = { "character_id" },
		referencedTable = "player_character", referencedColumns = { "id" })
@Entity
@Table(name = "quest_tracking", comment = "角色主线及支线地图指引偏好，不影响任务目标累计",
		uniqueConstraints = {
				@UniqueConstraint(name = "quest_tracking_character_id_key", columnNames = { "character_id" }) })
@Getter
@NoArgsConstructor
public class QuestTracking {

	/** 内部雪花主键，不作为客户端操作凭据。 */
	@Id
	@CosIdGenerated
	@Column(name = "id", nullable = false, comment = "CosId雪花主键")
	private Long id;

	/** 偏好所属角色，写入前必须取得该角色的事务锁。 */
	@Column(name = "character_id", nullable = false, comment = "偏好所属角色，每个角色只保存一份")
	private Long characterId;

	/** 主线整项任务编号；空值表示玩家明确取消主线追踪。 */
	@Column(name = "main_quest", length = 255, comment = "自选主线任务编号，空值表示取消追踪")
	private String mainQuest;

	/** 玩家自选的支线整项任务编号；空值表示不追踪支线。 */
	@Column(name = "side_quest", length = 255, comment = "自选支线任务编号，空值表示取消追踪")
	private String sideQuest;

	/** 偏好修改版本，避免其他标签页的旧请求覆盖较新的选择。 */
	@ColumnDefault("0")
	@Column(name = "revision", nullable = false, comment = "追踪偏好变更版本，拒绝其他页面的过期请求")
	private long revision;

	public static QuestTracking create(long characterId, String mainQuest) {
		var value = new QuestTracking();
		value.characterId = characterId;
		value.mainQuest = mainQuest;
		return value;
	}

	/** 紧邻成功写入的同值重试无副作用；更早的版本一律拒绝，防止旧请求恢复旧追踪。 */
	public void select(QuestType type, String quest, long expectedRevision) {
		String current = type == QuestType.MAIN ? mainQuest : sideQuest;
		if (revision == expectedRevision + 1 && Objects.equals(current, quest))
			return;
		if (revision != expectedRevision)
			throw new BusinessException(409, "任务追踪已变化，请刷新后操作");
		if (Objects.equals(current, quest))
			return;
		if (type == QuestType.MAIN)
			mainQuest = quest;
		else
			sideQuest = quest;
		revision++;
	}

}
