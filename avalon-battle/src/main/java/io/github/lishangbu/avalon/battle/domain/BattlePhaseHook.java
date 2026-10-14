package io.github.lishangbu.avalon.battle.domain;

import io.github.lishangbu.avalon.combat.CombatPhaseHook;

/** Avalon阶段扩展组件，只能接触纯状态及固定输入，不接收资产回调或数据库实体。 */
public interface BattlePhaseHook extends CombatPhaseHook<BattleState, BattleRoundInput> {

}
