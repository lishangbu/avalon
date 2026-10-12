package io.github.lishangbu.avalon.battle.application.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** 多人战局白名单：只公开提交状态，不公开任何成员选招、随机种子及敌方指令。 */
public record DungeonCombatView(String id, String run, int round, String status, String name, Instant deadline,
		String self, boolean canSubmit, List<DungeonCombatViewMember> members, List<DungeonCombatViewUnit> units,
		List<String> log, Map<String, Integer> supplies, String telegraph, List<BattleViewLoot> loot) {

}
