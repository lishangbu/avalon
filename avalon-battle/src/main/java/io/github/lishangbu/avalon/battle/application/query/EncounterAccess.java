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

	private final CharacterFactQueryService facts;

	public boolean allowed(long owner, BattleEncounter encounter) {
		return switch (encounter.id()) {
			case "SHALLOWS_PASSAGE_GUARD" -> quests.done(owner, encounter.requires()) && !quests.done(owner, "BQ072")
					&& facts.codes(owner).contains("SCENE:BQ072_NET");
			case "BAY_MARKER_GUARD" -> quests.done(owner, encounter.requires()) && !quests.done(owner, "BQ064");
			case "OLD_TRAIL_GUARD" -> quests.done(owner, encounter.requires()) && !quests.done(owner, "BQ053");
			case "GARDEN_WORK_ROUTE" -> quests.done(owner, encounter.requires()) && !quests.done(owner, "BQ044")
					&& facts.codes(owner).contains("SCENE:BQ044_ROUTE");
			case "ROWAN_TRIAL_DELIVERY" -> quests.done(owner, encounter.requires()) && !quests.done(owner, "BQ036")
					&& facts.codes(owner).contains("SCENE:BQ036_PARCEL");
			case "TUTORIAL" -> quests.done(owner, "BQ006");
			case "PATH_GUARD" ->
				quests.done(owner, encounter.requires()) && facts.codes(owner).contains("SCENE:BQ027_CLEAR_PATH");
			case "CAVE_BOSS" -> quests.done(owner, "BQ011") || cavePrepared(owner);
			default -> quests.done(owner, encounter.requires());
		};
	}

	/** 洞口资料直接提供；只有真实开战命令登记准备完成并结算任务。 */
	public boolean cavePrepared(long owner) {
		return quests.done(owner, "BQ010");
	}

}
