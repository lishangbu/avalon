package io.github.lishangbu.avalon.battle.application.model;

/**
 * BattleLobby 使用的独立数据投影，包含id、name、hp、maxHp等数据。 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param id 稳定业务编号，用于查询、关联或回执引用
 * @param name 展示名称
 * @param hp 当前生命值
 * @param maxHp 生命上限
 * @param mp 当前魔力值
 * @param maxMp 魔力上限
 */
public record BattleLobbyVitals(String id, String name, int hp, int maxHp, int mp, int maxMp) {
}
