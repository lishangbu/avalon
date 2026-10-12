package io.github.lishangbu.avalon.content.battle.model;

/**
 * DungeonCatalog 使用的类型化配置，声明kind、power、notice等数据。 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param kind 业务种类
 * @param power 威力
 * @param notice 玩家可读通知
 */
public record DungeonAction(String kind, int power, String notice) {
}
