package io.github.lishangbu.avalon.creature.application.model;

/**
 * EncyclopediaView 使用的独立数据投影，包含slot、name等数据。 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param slot 资产槽位
 * @param name 展示名称
 */
public record EncyclopediaViewAbility(String slot, String name) {
}
