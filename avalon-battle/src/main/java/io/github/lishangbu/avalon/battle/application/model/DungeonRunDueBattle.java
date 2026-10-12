package io.github.lishangbu.avalon.battle.application.model;

/**
 * DungeonRunQueryService 使用的独立数据投影，包含run、battle等数据。 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param run 副本运行引用
 * @param battle 战局引用或战局数据
 */
public record DungeonRunDueBattle(long run, long battle) {
}
