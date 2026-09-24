package io.github.lishangbu.avalon.world.application.command;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.content.quest.QuestRulesCatalog;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.world.application.query.QuestQueryService;
import io.github.lishangbu.avalon.world.application.query.QuestTrackingQueryService;
import io.github.lishangbu.avalon.world.persistence.entity.QuestTracking;
import io.github.lishangbu.avalon.world.persistence.repository.QuestTrackingRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 追踪独立于任务参与状态，换指引不会暂停其他任务，也不能借此解锁未开放剧情。 */
@Component
@RequiredArgsConstructor
public class QuestTrackingCommandService {

	private final CharacterCommandService characters;

	private final QuestQueryService quests;

	private final QuestTrackingQueryService queries;

	private final QuestTrackingRepository tracking;

	@Transactional
	public void select(long owner, Request request) {
		if (request == null || request.type() == null || request.expectedRevision() == null
				|| request.expectedRevision() < 0 || request.expectedRevision() == Long.MAX_VALUE)
			throw new BusinessException(400, "缺少有效的任务追踪参数");
		var character = characters.requireOutsideBattle(owner);
		if (request.quest() != null && quests.state(owner)
			.journal()
			.stream()
			.noneMatch(entry -> entry.id().equals(request.quest()) && entry.type() == request.type()
					&& QuestTrackingQueryService.eligible(entry)))
			throw new BusinessException(409, "此任务当前不可追踪");
		var value = tracking.findByCharacterId(character.id())
			.orElseGet(() -> tracking.save(QuestTracking.create(character.id(), queries.state(owner).main())));
		value.select(request.type(), request.quest(), request.expectedRevision());
	}

	public record Request(QuestRulesCatalog.Type type, String quest, Long expectedRevision) {
	}

}
