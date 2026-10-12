package io.github.lishangbu.avalon.battle.application.model;

/**
 * DungeonProgressQueryService 使用的独立数据投影，包含run、remainingStarts等数据。
 * 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param run 副本运行引用
 * @param remainingStarts 剩余开启次数
 */
public record DungeonProgressCurrent(DungeonRunView run, int remainingStarts) {
}
