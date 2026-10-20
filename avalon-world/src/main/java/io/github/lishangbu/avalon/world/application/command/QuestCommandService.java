package io.github.lishangbu.avalon.world.application.command;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.content.quest.QuestRulesCatalog;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.world.application.model.QuestResult;
import io.github.lishangbu.avalon.world.application.query.QuestDeliveryRequirements;
import io.github.lishangbu.avalon.world.persistence.entity.QuestJournalStateStatus;
import io.github.lishangbu.avalon.world.persistence.mapper.QuestResultCodec;
import io.github.lishangbu.avalon.world.persistence.repository.QuestJournalStateRepository;
import io.github.lishangbu.avalon.world.persistence.repository.QuestRepository;
import io.github.lishangbu.avalon.world.quest.QuestRuntimeService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 服务端玩法交付门面；显式检查入口资格，实例引擎负责唯一结算。 */
@Component
@RequiredArgsConstructor
public class QuestCommandService {

	private final CharacterCommandService characters;

	private final CharacterFactQueryService facts;

	private final QuestCatalog catalog;

	private final QuestRepository receipts;

	private final QuestDeliveryRequirements delivery;

	private final QuestResultCodec codec;

	private final QuestJournalStateRepository participation;

	private final QuestRuntimeService runtime;

	private final QuestRulesCatalog rules;

	/**
	 * 持角色锁后优先读取首次响应，已交付任务即使人物离场或配置奖励变化仍重放原结果。 首次请求的游戏门禁检查与实例结算共享整个事务，资产、进度、通知和回执任一失败均回滚。
	 */
	@Transactional
	public QuestResult interact(long owner, String stepId) {
		var character = characters.requireOutsideDungeon(owner);
		var prior = receipts.prior(character.id(), stepId);
		if (prior != null)
			return codec.decode(prior.getResponse());
		var step = catalog.step(stepId);
		if (step == null)
			throw new BusinessException(404, "交互不存在");
		if (!step.location().equals(character.location()))
			throw new BusinessException(409, "请先到达任务对应地点");
		if (!receipts.done(character.id()).containsAll(step.requires()))
			throw new BusinessException(409, "尚未满足任务前置条件");
		var codes = facts.codes(owner);
		if ((step.acceptance().explicit() || !step.acceptEffects().isEmpty())
				&& !codes.contains("QUEST_ACCEPT:" + step.id()))
			throw new BusinessException(409, "请先接取任务并领取出行补给");
		if (!catalog.receptionAvailable(step, codes::contains))
			throw new BusinessException(409, "接待人物当前不在此处");
		var state = participation.findByCharacterIdAndQuestCode(character.id(), step.quest()).orElse(null);
		if (state == null || state.getStatus() != QuestJournalStateStatus.ACTIVE)
			throw new BusinessException(409, "请先接取任务");
		String missing = delivery.missing(owner, step);
		if (missing != null)
			throw new BusinessException(409, missing);
		var result = runtime.deliver(owner, stepId);
		return new QuestResult(stepId, step.dialogue(), result.rewards());
	}

}
