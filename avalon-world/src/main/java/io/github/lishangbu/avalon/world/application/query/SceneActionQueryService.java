package io.github.lishangbu.avalon.world.application.query;

import io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.world.SceneActionCatalog;
import io.github.lishangbu.avalon.platform.BusinessException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/** 仅投影当前地点的场景操作，不返回报告正确答案或可由客户端提交的奖励。 */
@Component
@RequiredArgsConstructor
public class SceneActionQueryService {

	private final CharacterQueryService characters;

	private final CharacterFactQueryService facts;

	private final SceneActionCatalog catalog;

	private final QuestSceneAccess questAccess;

	@Transactional
	public State state(long owner) {
		var character = characters.find(owner);
		if (character == null)
			throw new BusinessException(404, "请先创建角色");
		var adventure = characters.adventure(owner);
		if (adventure.battle() != null || adventure.dungeon() != null || adventure.course() != null)
			throw new BusinessException(409, "请先结束当前战斗、副本或学院实践课");
		var completed = facts.codes(owner);
		var allowed = questAccess.available(characters.requireId(owner));
		var actions = catalog.actions()
			.stream()
			.filter(action -> action.location().equals(character.location()))
			.filter(action -> completed.contains("SCENE:" + action.id())
					|| allowed.contains(action.id()) && catalog.available(action, completed::contains))
			.map(action -> new View(action.id(), action.type(), action.label(), action.description(), action.choices(),
					completed.contains("SCENE:" + action.id()), action.npcId()))
			.toList();
		return new State(character.location(), character.revision(), actions);
	}

	public record State(String location, long revision, List<View> actions) {
	}

	public record View(String id, SceneActionCatalog.Type type, String label, String description,
			List<SceneActionCatalog.Choice> choices, boolean completed, String npcId) {
	}

}
