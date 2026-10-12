package io.github.lishangbu.avalon.battle.application.model;

/**
 * FoodQueryService 使用的独立数据投影，包含id、name、health、mana等数据。 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param id 稳定业务编号，用于查询、关联或回执引用
 * @param name 展示名称
 * @param health 生命属性
 * @param mana 魔力属性
 */
public record FoodTarget(String id, String name, int health, int mana, int maximumHealth, int maximumMana) {
}
