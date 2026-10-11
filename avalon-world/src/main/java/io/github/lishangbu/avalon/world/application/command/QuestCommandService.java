package io.github.lishangbu.avalon.world.application.command;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService;
import io.github.lishangbu.avalon.character.application.command.CharacterFactCommandService;
import io.github.lishangbu.avalon.character.application.model.CharacterContext;
import io.github.lishangbu.avalon.character.application.model.PlayerNotice;
import org.springframework.context.ApplicationEventPublisher;
import io.github.lishangbu.avalon.content.creature.CreatureCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.world.application.query.QuestDeliveryRequirements;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.world.application.model.QuestResult;
import io.github.lishangbu.avalon.world.persistence.entity.QuestReceipt;
import io.github.lishangbu.avalon.world.persistence.mapper.QuestResultCodec;
import io.github.lishangbu.avalon.world.persistence.repository.QuestRepository;
import io.github.lishangbu.avalon.world.persistence.repository.QuestParticipationRepository;
import io.github.lishangbu.avalon.world.persistence.entity.QuestParticipation;
import io.github.lishangbu.avalon.world.application.query.QuestObjectiveQueryService;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 通用任务执行器：角色归属、位置与前置校验后，顺序执行配置效果并保存一次性凭证。
 */
@Component
public class QuestCommandService {

	private final CharacterCommandService characters;

	private final CharacterFactQueryService facts;

	private final CharacterFactCommandService factCommands;

	private final QuestCatalog catalog;

	private final QuestRepository receipts;

	private final ItemCatalog itemCatalog;

	private final CreatureCatalog creatureCatalog;

	private final InventoryCommandService inventory;

	private final CreatureCommandService creatures;

	private final QuestDeliveryRequirements delivery;

	private final QuestResultCodec codec;

	private final QuestParticipationRepository participation;

	private final QuestObjectiveQueryService objectives;

	/** 任务交付事件仅在首次成功步骤发布；通知持久化失败会回滚奖励与完成凭证。 */
	private final ApplicationEventPublisher notices;

	@Autowired
	public QuestCommandService(CharacterCommandService characters, CharacterFactQueryService facts,
			CharacterFactCommandService factCommands, QuestCatalog catalog, QuestRepository receipts,
			ItemCatalog itemCatalog, CreatureCatalog creatureCatalog, InventoryCommandService inventory,
			CreatureCommandService creatures, QuestDeliveryRequirements delivery, QuestResultCodec codec,
			QuestParticipationRepository participation, QuestObjectiveQueryService objectives,
			ApplicationEventPublisher notices) {
		this.characters = characters;
		this.facts = facts;
		this.factCommands = factCommands;
		this.catalog = catalog;
		this.receipts = receipts;
		this.itemCatalog = itemCatalog;
		this.creatureCatalog = creatureCatalog;
		this.inventory = inventory;
		this.creatures = creatures;
		this.delivery = delivery;
		this.codec = codec;
		this.participation = participation;
		this.objectives = objectives;
		this.notices = notices;
	}

	/** 整个步骤只有一个事务；效果失败时经验、物品、幻兽与进度全部回滚。 */
	/**
	 * 任务命令唯一事务入口。先锁角色并查已有步骤凭证，重试直接重放原响应；首次执行再验证地点与前置。按 JSON
	 * 效果顺序执行，最后保存凭证。任一效果或凭证写入失败都会回滚整笔奖励。
	 */
	@Transactional
	public QuestResult interact(long owner, String stepId) {
		var character = characters.requireOutsideDungeon(owner);
		long id = character.id();
		QuestReceipt prior = receipts.prior(id, stepId);
		// 重试读取原响应，不能再执行效果；该分支也允许离开原地点后恢复响应。
		if (prior != null)
			return replay(prior);
		var step = catalog.step(stepId);
		if (step == null)
			throw new BusinessException(404, "交互不存在");
		if (!step.location().equals(character.location()))
			throw new BusinessException(409, "请先到达任务对应地点");
		if (!receipts.done(id).containsAll(step.requires()))
			throw new BusinessException(409, "尚未满足任务前置条件");
		var completedFacts = facts.codes(owner);
		if (!step.acceptEffects().isEmpty() && !completedFacts.contains("QUEST_ACCEPT:" + step.id()))
			throw new BusinessException(409, "请先接取任务并领取出行补给");
		if (!catalog.receptionAvailable(step, completedFacts::contains))
			throw new BusinessException(409, "接待人物当前不在此处");
		var state = participation.findByCharacterIdAndQuestCode(id, step.quest()).orElse(null);
		if (state == null || state.getStatus() != QuestParticipation.Status.ACTIVE)
			throw new BusinessException(409, "请先接取任务");
		if (!QuestObjectiveQueryService.factsFor(step, completedFacts, objectives.progress(id))
			.containsAll(step.facts()))
			throw new BusinessException(409, "请先完成任务要求的实际行动");
		String missing = delivery.missing(owner, step);
		if (missing != null)
			throw new BusinessException(409, missing);
		List<String> rewards = applyEffects(owner, character, step);
		QuestResult result = new QuestResult(stepId, step.dialogue(), rewards);
		saveReceipt(id, step, result);
		if (catalog.completedQuests(receipts.done(id)).contains(step.quest()))
			state.complete();
		factCommands.record(owner, "QUEST:" + step.id());
		// 任务标题来自校验过的内容目录；通知仅给本人，普通完成不会产生全服公告。
		notices.publishEvent(new PlayerNotice(owner, "QUEST:" + id + ":" + step.id(), "任务交付完成：" + step.title() + "。"));
		return result;
	}

	/**
	 * 按 JSON 顺序执行效果；经验奖励可能先改变携带容量，所以发宠时重新读取持锁角色等级。
	 */
	private List<String> applyEffects(long owner, CharacterContext character, QuestCatalog.Step step) {
		long id = character.id();
		List<String> rewards = new ArrayList<>();
		int index = 0;
		for (var effect : step.effects()) {
			String source = step.id() + ":" + index++;
			switch (effect.kind()) {
				case EXPERIENCE -> {
					characters.grantExperience(owner, effect.amount());
					creatures.grantPartyExperience(owner, effect.amount());
					rewards.add("经验 +" + effect.amount());
				}
				case GOLD -> {
					characters.changeGold(owner, effect.amount());
					rewards.add("金币 +" + effect.amount());
				}
				case GIVE_ITEM -> {
					inventory.grant(owner, source, effect.reference(), effect.amount(), false);
					rewards.add(itemCatalog.item(effect.reference()).name() + " ×" + effect.amount());
				}
				case TAKE_ITEM -> inventory.take(owner, effect.reference(), effect.amount());
				case GIVE_PET -> {
					creatures.grant(owner, source, effect.reference());
					rewards.add(creatureCatalog.pet(effect.reference()).name() + "加入队伍");
				}
				case GIVE_EQUIPMENT -> {
					inventory.grant(owner, source, effect.reference(), effect.amount(), true);
					var gear = itemCatalog.equipment(effect.reference());
					rewards.add(gear.names().getOrDefault(character.profession(), gear.name()));
				}
			}
		}
		return List.copyOf(rewards);
	}

	/**
	 * 凭证与奖励在同一事务落库；最终 flush 的约束错误会回滚前面全部效果。
	 */
	private void saveReceipt(long id, QuestCatalog.Step step, QuestResult result) {
		QuestReceipt receipt = new QuestReceipt();

		receipt.setCharacterId(id);
		receipt.setStep(step.id());
		receipt.setQuestCode(step.quest());
		receipt.setCompleted(step.completes());
		receipt.setCreatedAt(Instant.now());
		receipt.setResponse(codec.encode(result));
		receipts.saveAndFlush(receipt);
	}

	/**
	 * 读取已完成步骤的首次回执，不再查当前地点或重算奖励。回执损坏会抛内部异常，绝不能通过重新发奖修复。
	 */
	private QuestResult replay(QuestReceipt receipt) {
		return codec.decode(receipt.getResponse());
	}

}
