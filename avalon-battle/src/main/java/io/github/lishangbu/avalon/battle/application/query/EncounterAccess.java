package io.github.lishangbu.avalon.battle.application.query;

import io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService;
import io.github.lishangbu.avalon.content.battle.model.BattleEncounter;
import io.github.lishangbu.avalon.world.application.query.QuestQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 页面入口与开战命令共用任务门槛，读取准备记录不会产生任务事实或发放奖励。 */
@Component
@RequiredArgsConstructor
public class EncounterAccess {

	private final QuestQueryService quests;

	public boolean allowed(long owner, BattleEncounter encounter) {
		return quests.done(owner, encounter.requires());
	}

}
