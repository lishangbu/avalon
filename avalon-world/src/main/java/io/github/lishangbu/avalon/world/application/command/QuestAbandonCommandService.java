package io.github.lishangbu.avalon.world.application.command;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.content.quest.QuestRulesCatalog;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.world.application.model.QuestAbandonRequest;
import io.github.lishangbu.avalon.world.persistence.entity.QuestJournalStateStatus;
import io.github.lishangbu.avalon.world.persistence.repository.QuestJournalStateRepository;
import io.github.lishangbu.avalon.world.quest.QuestRuntimeService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 放弃只改变任务参与状态。保留步骤、目标和已消耗物品，不删除永久事实或返还补给。 */
@Component
@RequiredArgsConstructor
public class QuestAbandonCommandService {

	private final CharacterCommandService characters;

	private final QuestJournalStateRepository participation;

	private final QuestRulesCatalog rules;

	private final QuestRuntimeService runtime;

	/** 乐观版本配合角色锁，避免旧页面的放弃请求误伤刚刚重接的任务。 */
	@Transactional
	public void abandon(long owner, String quest, QuestAbandonRequest request) {
		if (request == null || request.expectedRevision() == null || request.expectedRevision() < 0)
			throw new BusinessException(400, "缺少有效的任务状态版本");
		var character = characters.requireOutsideDungeon(owner);
		var state = participation.findByCharacterIdAndQuestCode(character.id(), quest)
			.orElseThrow(() -> new BusinessException(409, "任务尚未接取"));
		long expected = request.expectedRevision();
		// 仅认可紧邻上一次状态的重试；放弃后重接会再次增加版本，旧请求不能再次放弃。
		if (state.getStatus() == QuestJournalStateStatus.ABANDONED && state.getRevision() == expected + 1)
			return;
		if (state.getRevision() != expected)
			throw new BusinessException(409, "任务状态已变化，请刷新后操作");
		runtime.abandon(owner, quest);
	}

}
