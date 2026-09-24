package io.github.lishangbu.avalon.battle.application.model;

import io.github.lishangbu.avalon.battle.domain.BattleState.Stats;

import java.util.List;
import java.util.Map;

/**
 * 公开战局严格白名单；敌方特性、槽位、下一轮指令、种子均不下发。
 */
public record BattleView(String id, int round, String status, String name, List<UnitView> units, List<String> log,
		Map<String, Integer> supplies, List<CaptureSupply> captureSupplies, String telegraph, boolean training,
		AcademyCapture academyCapture, List<Loot> loot) {

	public record Loot(String code, String name, int quantity) {
	}

	/** 只有捕获之后才公开临时个体槽位；该结果不代表新增真实资产。 */
	public record AcademyCapture(boolean viewed, String selectedBall, String targetId, String instruction,
			List<String> balls, String capturedAbilitySlot, String capturedAbility) {
	}

	/** 库存独立于当前捕捉资格；等级不满足或没有可捕目标时仍可查看持有数量。 */
	public record CaptureSupply(String code, String name, int level, int quantity) {
	}

	public record UnitView(String id, String name, String element, int level, boolean enemy, boolean hero, int position,
			int hp, int mp, Stats stats, boolean captured, String ability, int shield, int slowRounds,
			List<String> effects, List<Action> actions, List<CaptureOption> captures, Stats captureStats) {
	}

	/** 球档位独立展示，新增档位不增加固定字段；无库存或不可捕捉时不生成选项。 */
	public record CaptureOption(String code, String name, int level, double chance) {
	}

	public record Action(String id, String name, String target, int mana, int cooldown, boolean available) {
	}
}
