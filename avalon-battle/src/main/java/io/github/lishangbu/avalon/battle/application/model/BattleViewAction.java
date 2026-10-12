package io.github.lishangbu.avalon.battle.application.model;

/**
 * BattleView 使用的独立数据投影，包含id、name、target、mana等数据。 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param id 稳定业务编号，用于查询、关联或回执引用
 * @param name 展示名称
 * @param target 目标编号或目标声明
 * @param mana 魔力属性
 * @param cooldown 冷却回合数
 * @param available 当前是否满足可用条件
 */
public record BattleViewAction(String id, String name, String target, int mana, int cooldown, boolean available) {
}
