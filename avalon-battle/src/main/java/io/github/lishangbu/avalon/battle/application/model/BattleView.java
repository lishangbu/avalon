package io.github.lishangbu.avalon.battle.application.model;

import java.util.List;
import java.util.Map;

/**
 * 公开战局严格白名单；敌方特性、槽位、下一轮指令、种子均不下发。
 */
public record BattleView(String id, int round, int revision, String status, String name, List<BattleViewUnitView> units,
		List<String> log, Map<String, Integer> supplies, List<BattleViewCaptureSupply> captureSupplies,
		String telegraph, boolean training, BattleViewAcademyCapture academyCapture, List<BattleViewLoot> loot,
		boolean pvp) {

}
