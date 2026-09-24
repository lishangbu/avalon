package io.github.lishangbu.avalon.world.application.command;

import io.github.lishangbu.avalon.character.application.model.CharacterActionRecorded;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.world.application.query.QuestObjectiveQueryService;
import io.github.lishangbu.avalon.world.persistence.entity.QuestObjective;
import io.github.lishangbu.avalon.world.persistence.entity.QuestParticipation;
import io.github.lishangbu.avalon.world.persistence.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import java.util.stream.Collectors;

/** 同步消费真实角色行为，逐项核验参与资格；不向队友、尚未开启的步骤或已放弃任务广播进度。 */
@Component
@RequiredArgsConstructor
public class QuestObjectiveObserver {

	private final QuestCatalog catalog;

	private final QuestParticipationRepository participation;

	private final QuestRepository receipts;

	private final QuestObjectiveRepository objectives;

	@EventListener
	@Transactional(Transactional.TxType.MANDATORY)
	public void recorded(CharacterActionRecorded event) {
		if (QuestObjectiveQueryService.historical(event.code()))
			return;
		var targets = catalog.definition()
			.steps()
			.stream()
			.filter(step -> step.facts().contains(event.code()))
			.toList();
		if (targets.isEmpty())
			return;
		var active = participation.findByCharacterId(event.characterId())
			.stream()
			.filter(state -> state.getStatus() == QuestParticipation.Status.ACTIVE)
			.map(QuestParticipation::getQuestCode)
			.collect(Collectors.toSet());
		var done = receipts.done(event.characterId());
		for (var step : targets) {
			if (active.contains(step.quest()) && !done.contains(step.id()) && done.containsAll(step.requires())
					&& !objectives.existsByCharacterIdAndStepAndFact(event.characterId(), step.id(), event.code()))
				objectives.save(new QuestObjective(event.characterId(), step.id(), event.code()));
		}
	}

}
