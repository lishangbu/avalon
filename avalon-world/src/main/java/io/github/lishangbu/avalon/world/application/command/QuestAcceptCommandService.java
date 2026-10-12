package io.github.lishangbu.avalon.world.application.command;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.command.CharacterFactCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.content.quest.QuestDialogueCatalog;
import io.github.lishangbu.avalon.content.quest.QuestRulesCatalog;
import io.github.lishangbu.avalon.content.quest.model.QuestRepeatPolicy;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.quest.model.QuestStatus;
import io.github.lishangbu.avalon.world.application.model.QuestAcceptRequest;
import io.github.lishangbu.avalon.world.application.model.QuestAcceptResult;
import io.github.lishangbu.avalon.world.persistence.entity.QuestAcceptReceipt;
import io.github.lishangbu.avalon.world.persistence.entity.QuestParticipationStatus;
import io.github.lishangbu.avalon.world.persistence.repository.QuestAcceptReceiptRepository;
import io.github.lishangbu.avalon.world.persistence.repository.QuestParticipationRepository;
import io.github.lishangbu.avalon.world.persistence.repository.QuestRepository;
import io.github.lishangbu.avalon.world.quest.QuestClock;
import io.github.lishangbu.avalon.world.quest.QuestRuntimeService;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** 显式接取任务的写入口。补给、接取事实和回执同事务，满包使用正式待领路径。 */
@Component
@RequiredArgsConstructor
public class QuestAcceptCommandService {

	private final CharacterCommandService characters;

	private final CharacterQueryService queries;

	private final CharacterFactQueryService facts;

	private final CharacterFactCommandService factCommands;

	private final QuestCatalog catalog;

	private final QuestDialogueCatalog dialogues;

	private final ItemCatalog items;

	private final InventoryCommandService inventory;

	private final QuestRepository completion;

	private final QuestAcceptReceiptRepository receipts;

	private final ObjectMapper json;

	private final QuestParticipationRepository participation;

	private final QuestRuntimeService runtime;

	private final QuestRulesCatalog rules;

	/** 接取时刻与核心周期、截止计算共用服务器任务时钟。 */
	private final QuestClock clock;

	/** 请求回放先于位置检查；首次接取在角色锁内验证地点、依赖并保存本实例本轮补给回执。 */
	@Transactional
	public QuestAcceptResult accept(long owner, String stepId, QuestAcceptRequest request) {
		validate(stepId, request);
		var character = characters.lock(owner);
		String payload = json.writeValueAsString(new Payload(stepId, request.expectedRevision()));
		var prior = receipts.prior(character.id(), request.requestId());
		if (prior != null) {
			if (!payload.equals(prior.getPayload()))
				throw new BusinessException(409, "同一请求号不能接取不同任务或位置版本");
			return json.readValue(prior.getResponse(), QuestAcceptResult.class);
		}
		characters.requireOutsideDungeon(owner);
		if (queries.find(owner).revision() != request.expectedRevision())
			throw new BusinessException(409, "位置已变化，请刷新地图");
		var step = catalog.step(stepId);
		if (step == null)
			throw new BusinessException(404, "任务不存在");
		if (!step.acceptance().location().equals(character.location()))
			throw new BusinessException(409, "请先到达任务接取地点");
		var done = completion.done(character.id());
		boolean repeated = rules.rule(step.quest()).repeatPolicy() != QuestRepeatPolicy.ONCE;
		// 限时一次性任务也可能重试：本次实例进度优先于永久剧情回执。
		// 引擎投影允许重新接取入口时，旧尝试已完成的步骤不能阻挡新尝试。
		var projected = runtime.steps(owner).get(stepId);
		boolean restarting = projected != null && projected.available() && !projected.accepted()
				&& projected.instance().status() != QuestStatus.PAUSED;
		var currentDone = restarting ? Set.<String>of() : runtime.done(owner, step.quest());
		boolean prerequisites = step.requires().stream().allMatch(required -> {
			var dependency = catalog.step(required);
			return dependency != null && dependency.quest().equals(step.quest()) ? currentDone.contains(required)
					: done.contains(required);
		});
		if ((!repeated && !restarting && currentDone.contains(stepId)) || !prerequisites)
			throw new BusinessException(409, "任务已完成或尚未满足接取前置");
		var codes = facts.codes(owner);
		if (!catalog.acceptanceAvailable(step, codes::contains))
			throw new BusinessException(409, "接待人物当前不在此处");
		// 参与状态属于整项任务，后续步骤领取补给不能创建另一份任务或重置接取窗口。
		var state = participation.findByCharacterIdAndQuestCode(character.id(), step.quest()).orElse(null);
		boolean resuming = state != null && state.getStatus() == QuestParticipationStatus.ABANDONED;
		var attempt = runtime.accept(owner, step.quest());
		var accepted = receipts.accepted(character.id(), stepId)
			.stream()
			.filter(receipt -> attempt.instance().id().equals(receipt.getRuntimeId())
					&& receipt.getRuntimeRound() == attempt.instance().round()
					|| receipt.getRuntimeId() == null && !attempt.fresh() && !repeated)
			.toList();
		Instant acceptedAt;
		List<String> rewards = new ArrayList<>(attempt.rewards());
		if (accepted.isEmpty()) {
			if (!repeated && !attempt.fresh() && attempt.instance().round() == 1
					&& codes.contains("QUEST_ACCEPT:" + stepId))
				throw new IllegalStateException("接取事实存在但接取回执缺失，不能重新发补给");
			acceptedAt = clock.now();
			int index = 0;
			for (var effect : step.acceptEffects()) {
				inventory.grant(owner, "QUEST_ACCEPT:" + attempt.instance().id() + ":" + attempt.instance().round()
						+ ":" + stepId + ":" + index++, effect.reference(), effect.amount(), false);
				rewards.add(items.item(effect.reference()).name() + " ×" + effect.amount());
			}
			factCommands.record(owner, "QUEST_ACCEPT:" + stepId);
		}
		else
			acceptedAt = accepted.getFirst().getAcceptedAt();
		var text = dialogues.step(stepId);
		var result = new QuestAcceptResult(stepId, true, resuming ? text.resume() : text.accepted(), rewards,
				acceptedAt);
		var receipt = new QuestAcceptReceipt();
		receipt.setRuntimeId(attempt.instance().id());
		receipt.setRuntimeRound(attempt.instance().round());
		receipt.setCharacterId(character.id());
		receipt.setRequestId(request.requestId());
		receipt.setStep(stepId);
		receipt.setPayload(payload);
		receipt.setAcceptedAt(acceptedAt);
		receipt.setResponse(json.writeValueAsString(result));
		receipts.saveAndFlush(receipt);
		return result;
	}

	/** 标准UUID及位置修订是幂等载荷的一部分，缺失或不规范参数在任何资产操作前拒绝。 */
	private void validate(String stepId, QuestAcceptRequest request) {
		if (stepId == null || stepId.isBlank() || request == null || request.requestId() == null
				|| request.expectedRevision() == null || request.expectedRevision() < 0)
			throw new BusinessException(400, "任务接取请求不完整");
		try {
			if (!UUID.fromString(request.requestId()).toString().equals(request.requestId()))
				throw new IllegalArgumentException();
		}
		catch (IllegalArgumentException error) {
			throw new BusinessException(400, "任务接取请求号必须为标准UUID");
		}
	}

	/** 用规范JSON固定首次请求含义，同一请求号不能用于其他步骤或位置修订。 */
	private record Payload(String step, long expectedRevision) {
	}

}
