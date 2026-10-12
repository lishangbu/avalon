package io.github.lishangbu.avalon.creature.application.model;

/**
 * CreatureNamingQueryService 使用的独立数据投影，包含name、renamed、price、maxLength等数据。
 * 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param name 展示名称
 * @param price 价格
 */
public record CreatureNamingPreview(String name, int renamed, int price, int maxLength) {
}
