package io.github.lishangbu.avalon.world.application.command;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.content.quest.QuestRulesCatalog;
import io.github.lishangbu.avalon.content.quest.model.QuestRepeatPolicy;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.world.application.model.QuestResult;
import io.github.lishangbu.avalon.world.application.query.QuestDeliveryRequirements;
import io.github.lishangbu.avalon.world.persistence.entity.QuestParticipationStatus;
import io.github.lishangbu.avalon.world.persistence.mapper.QuestResultCodec;
import io.github.lishangbu.avalon.world.persistence.repository.QuestParticipationRepository;
import io.github.lishangbu.avalon.world.persistence.repository.QuestRepository;
import io.github.lishangbu.avalon.world.quest.QuestRuntimeService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 一次性任务的兼容交付入口。保留旧地点、人物和补给错误提示以及首次响应重放， 实际目标、条件、效果、完成回执和通知统一由实例宿主处理，不保留第二套奖励执行逻辑。
 * 重复任务必须走携带实例及轮次的新命令，不能让旧请求误推进下一轮。
 */
@Component
@RequiredArgsConstructor
public class QuestCommandService {

	private final CharacterCommandService characters;

	private final CharacterFactQueryService facts;

	private final QuestCatalog catalog;

	private final QuestRepository receipts;

	private final QuestDeliveryRequirements delivery;

	private final QuestResultCodec codec;

	private final QuestParticipationRepository participation;

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
		if (rules.rule(step.quest()).repeatPolicy() != QuestRepeatPolicy.ONCE)
			throw new BusinessException(400, "重复任务请使用实例交付接口");
		if (!step.location().equals(character.location()))
			throw new BusinessException(409, "请先到达任务对应地点");
		if (!receipts.done(character.id()).containsAll(step.requires()))
			throw new BusinessException(409, "尚未满足任务前置条件");
		var codes = facts.codes(owner);
		if (!step.acceptEffects().isEmpty() && !codes.contains("QUEST_ACCEPT:" + step.id()))
			throw new BusinessException(409, "请先接取任务并领取出行补给");
		if (!catalog.receptionAvailable(step, codes::contains))
			throw new BusinessException(409, "接待人物当前不在此处");
		var state = participation.findByCharacterIdAndQuestCode(character.id(), step.quest()).orElse(null);
		if (state == null || state.getStatus() != QuestParticipationStatus.ACTIVE)
			throw new BusinessException(409, "请先接取任务");
		String missing = delivery.missing(owner, step);
		if (missing != null)
			throw new BusinessException(409, missing);
		var result = runtime.deliver(owner, stepId);
		return new QuestResult(stepId, step.dialogue(), result.rewards());
	}

}
