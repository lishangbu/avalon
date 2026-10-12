package io.github.lishangbu.avalon.world.application.query;

import io.github.lishangbu.avalon.world.application.model.QuestConversationStage;

import io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.content.quest.QuestDialogueCatalog;
import io.github.lishangbu.avalon.content.quest.model.QuestContentStep;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.world.application.model.QuestConversation;
import io.github.lishangbu.avalon.world.persistence.repository.QuestRepository;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 只读的现场交谈投影，不接取、不领奖、不改变角色或全服状态。 */
@Component
@RequiredArgsConstructor
public class QuestConversationQueryService {

	private final CharacterQueryService characters;

	private final CharacterFactQueryService facts;

	private final QuestQueryService quests;

	private final QuestRepository receipts;

	private final QuestCatalog catalog;

	private final QuestDialogueCatalog dialogues;

	@Transactional
	public List<QuestConversation> local(long owner) {
		var character = characters.find(owner);
		if (character == null)
			throw new BusinessException(404, "请先创建角色");
		var state = quests.state(owner);
		var codes = facts.codes(owner);
		var result = new ArrayList<QuestConversation>();
		var speaking = new HashSet<String>();
		for (var action : state.available()) {
			if (!action.location().equals(character.location()))
				continue;
			var step = catalog.step(action.id());
			if (action.accepted() ? !catalog.receptionAvailable(step, codes::contains) : !action.acceptAvailable())
				continue;
			var text = dialogues.step(step.id());
			boolean abandoned = state.journal()
				.stream()
				.anyMatch(entry -> entry.id().equals(step.quest()) && "ABANDONED".equals(entry.status()));
			QuestConversationStage stage = !action.accepted()
					? (abandoned ? QuestConversationStage.RESUME : QuestConversationStage.OFFER)
					: action.interactable() ? QuestConversationStage.READY : QuestConversationStage.PROGRESS;
			String message = switch (stage) {
				case OFFER -> text.offer();
				case RESUME -> text.resume();
				case READY -> text.ready();
				default -> text.progress();
			};
			String speaker = action.npcId() == null ? "现场" : action.npc();
			speaking.add(action.npcId() == null ? "" : action.npcId());
			result.add(new QuestConversation(step.id(), step.title(), speaker, stage, message, action.npcId()));
		}
		// 每位在场人物只保留最近一个已完成步骤的再访反馈；若有当前委托，优先交谈当前委托。
		var revisits = new LinkedHashMap<String, QuestContentStep>();
		var done = receipts.done(characters.requireId(owner));
		for (var step : catalog.definition().steps()) {
			String speaker = step.npcId() == null ? "" : step.npcId();
			if (done.contains(step.id()) && step.location().equals(character.location()) && !speaking.contains(speaker)
					&& catalog.receptionAvailable(step, codes::contains))
				revisits.put(speaker, step);
		}
		for (var step : revisits.values())
			result
				.add(new QuestConversation(step.id(), step.title(), step.npcId() == null ? "现场" : catalog.npcName(step),
						QuestConversationStage.REVISIT, dialogues.step(step.id()).revisit(), step.npcId()));
		return List.copyOf(result);
	}

}
