package io.github.lishangbu.avalon.battle.application.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** 多人战局白名单：只公开提交状态，不公开任何成员选招、随机种子及敌方指令。 */
public record DungeonCombatView(String id, String run, int round, String status, String name, Instant deadline,
		String self, boolean canSubmit, List<Member> members, List<Unit> units, List<String> log,
		Map<String, Integer> supplies, String telegraph, List<BattleView.Loot> loot) {
	public record Member(String id, boolean submitted, boolean removed, boolean leaving, int missedRounds) {
	}

	/** 主人编号是公开角色编号；只有本人单位携带操作菜单。 */
	public record Unit(String owner, boolean controllable, BattleView.UnitView detail) {
	}
}
