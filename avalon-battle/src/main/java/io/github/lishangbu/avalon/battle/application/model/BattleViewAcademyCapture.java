package io.github.lishangbu.avalon.battle.application.model;

import java.util.List;

/** 只有捕获之后才公开临时个体槽位；该结果不代表新增真实资产。 */
public record BattleViewAcademyCapture(boolean viewed, String selectedBall, String targetId, String instruction,
		List<String> balls, String capturedAbilitySlot, String capturedAbility) {
}
