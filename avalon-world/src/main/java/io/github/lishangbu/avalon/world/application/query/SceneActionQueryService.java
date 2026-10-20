package io.github.lishangbu.avalon.world.application.query;

import io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.world.SceneActionCatalog;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.world.application.model.SceneActionInformationView;
import io.github.lishangbu.avalon.world.application.model.SceneActionState;
import io.github.lishangbu.avalon.world.application.model.SceneActionView;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 仅投影当前地点的场景操作，不返回报告正确答案或可由客户端提交的奖励。 */
@Component
@RequiredArgsConstructor
public class SceneActionQueryService {

	private final CharacterQueryService characters;

	private final CharacterFactQueryService facts;

	private final SceneActionCatalog catalog;

	private final QuestSceneAccess questAccess;

	@Transactional
	public SceneActionState state(long owner) {
		var character = characters.find(owner);
		if (character == null)
			throw new BusinessException(404, "请先创建角色");
		var adventure = characters.adventure(owner);
		if (adventure.battle() != null || adventure.dungeon() != null || adventure.course() != null)
			throw new BusinessException(409, "请先结束当前战斗、副本或学院实践课");
		// 完成状态与首次时间必须来自同一次事实查询：默认隔离级别下，两次查询间可能有新行动提交。
		// 从时间映射取键集合，避免把一个查询的完成状态与另一个查询的时间拼成不一致响应，同时减少一次读库。
		// 读取和回看均不创建新记录，断线刷新仍显示原有时间。
		var recordedTimes = facts.recordedTimes(owner);
		var completed = recordedTimes.keySet();
		var allowed = questAccess.available(characters.requireId(owner));
		var actions = catalog.actions()
			.stream()
			.filter(action -> action.location().equals(character.location()))
			.filter(action -> completed.contains("SCENE:" + action.id())
					|| allowed.contains(action.id()) && catalog.available(action, completed::contains))
			.map(action -> new SceneActionView(action.id(), action.type(), action.label(), action.description(),
					action.choices(), completed.contains("SCENE:" + action.id()), action.npcId(),
					recordedTimes.get("SCENE:" + action.id())))
			.toList();
		var information = catalog.information()
			.stream()
			.filter(entry -> entry.location().equals(character.location()))
			.filter(entry -> catalog.available(entry, completed::contains))
			.map(entry -> new SceneActionInformationView(entry.id(), entry.label(), entry.description(), entry.npcId()))
			.toList();
		return new SceneActionState(character.location(), character.revision(), actions, information);
	}

}
