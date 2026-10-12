package io.github.lishangbu.avalon.battle.application.model;

/** 主人编号是公开角色编号；只有本人单位携带操作菜单。 */
public record DungeonCombatViewUnit(String owner, boolean controllable, BattleViewUnitView detail) {
}
