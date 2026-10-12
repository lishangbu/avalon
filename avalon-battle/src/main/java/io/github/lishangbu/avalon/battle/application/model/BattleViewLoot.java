package io.github.lishangbu.avalon.battle.application.model;

/**
 * BattleView 使用的独立数据投影，包含code、name、quantity等数据。 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param code 内容目录中的稳定代码
 * @param name 展示名称
 * @param quantity 物品或资源数量
 */
public record BattleViewLoot(String code, String name, int quantity) {
}
