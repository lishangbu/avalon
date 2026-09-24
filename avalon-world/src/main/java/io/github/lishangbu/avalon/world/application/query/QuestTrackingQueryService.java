package io.github.lishangbu.avalon.world.application.query;

import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.quest.QuestRulesCatalog;
import io.github.lishangbu.avalon.world.application.model.QuestJournalEntry;
import io.github.lishangbu.avalon.world.application.model.QuestTrackingView;
import io.github.lishangbu.avalon.world.persistence.repository.QuestTrackingRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;

/** 追踪读侧不会接取任务或修改偏好，完成及放弃的选择立即从指引中隐藏。 */
@Component
@RequiredArgsConstructor
public class QuestTrackingQueryService {

	private final CharacterQueryService characters;

	private final QuestQueryService quests;

	private final QuestTrackingRepository tracking;

	@Transactional
	public QuestTrackingView state(long owner) {
		var journal = quests.state(owner).journal();
		var saved = tracking.findByCharacterId(characters.requireId(owner));
		// 从未设置偏好的角色按任务顺序获得一条主线指引；显式取消后不能被读取操作重新开启。
		if (saved.isEmpty())
			return new QuestTrackingView(journal.stream()
				.filter(entry -> eligible(entry) && entry.type() == QuestRulesCatalog.Type.MAIN)
				.map(QuestJournalEntry::id)
				.findFirst()
				.orElse(null), null, 0);
		var value = saved.get();
		return new QuestTrackingView(visible(journal, value.getMainQuest()), visible(journal, value.getSideQuest()),
				value.getRevision());
	}

	public static boolean eligible(QuestJournalEntry entry) {
		return !entry.completed() && !"ABANDONED".equals(entry.status());
	}

	private String visible(List<QuestJournalEntry> journal, String id) {
		return journal.stream().anyMatch(entry -> entry.id().equals(id) && eligible(entry)) ? id : null;
	}

}
