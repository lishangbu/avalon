package io.github.lishangbu.avalon.market.application.model;

/**
 * MarketQueryService 使用的独立数据投影，包含species、hidden、skill等数据。
 * 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param species 幻兽物种代码
 * @param skill 技能代码
 */
public record MarketCreatureFilter(String species, Boolean hidden, String skill) {
}
