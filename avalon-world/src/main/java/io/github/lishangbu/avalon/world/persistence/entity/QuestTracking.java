package io.github.lishangbu.avalon.world.persistence.entity;

import io.github.lishangbu.avalon.content.quest.QuestRulesCatalog;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.platform.persistence.CosIdGenerated;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.Objects;

/** 角色的地图指引偏好。两个固定槽位只控制显示，不参与目标累计与交付资格判定。 */
@Entity
@Table(name = "quest_tracking", uniqueConstraints = @UniqueConstraint(columnNames = "character_id"))
@Getter
@NoArgsConstructor
public class QuestTracking {

	/** 内部雪花主键，不作为客户端操作凭据。 */
	@Id
	@CosIdGenerated
	private Long id;

	/** 偏好所属角色，写入前必须取得该角色的事务锁。 */
	@Column(name = "character_id", nullable = false)
	private Long characterId;

	/** 主线整项任务编号；空值表示玩家明确取消主线追踪。 */
	@Column(name = "main_quest")
	private String mainQuest;

	/** 玩家自选的支线整项任务编号；空值表示不追踪支线。 */
	@Column(name = "side_quest")
	private String sideQuest;

	/** 偏好修改版本，避免其他标签页的旧请求覆盖较新的选择。 */
	@Column(nullable = false)
	private long revision;

	public static QuestTracking create(long characterId, String mainQuest) {
		var value = new QuestTracking();
		value.characterId = characterId;
		value.mainQuest = mainQuest;
		return value;
	}

	/** 紧邻成功写入的同值重试无副作用；更早的版本一律拒绝，防止旧请求恢复旧追踪。 */
	public void select(QuestRulesCatalog.Type type, String quest, long expectedRevision) {
		String current = type == QuestRulesCatalog.Type.MAIN ? mainQuest : sideQuest;
		if (revision == expectedRevision + 1 && Objects.equals(current, quest))
			return;
		if (revision != expectedRevision)
			throw new BusinessException(409, "任务追踪已变化，请刷新后操作");
		if (Objects.equals(current, quest))
			return;
		if (type == QuestRulesCatalog.Type.MAIN)
			mainQuest = quest;
		else
			sideQuest = quest;
		revision++;
	}

}
