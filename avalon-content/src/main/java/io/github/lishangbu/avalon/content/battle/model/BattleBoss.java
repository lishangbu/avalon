package io.github.lishangbu.avalon.content.battle.model;

/** 剧情首领为独立模板，不伪装成可捕获物种，脚本及奖励步骤引用均启动校验。 */
public record BattleBoss(String name, String element, String policy, int level, int health, int attack, int defense,
		int speed, String rewardStep) {
}
