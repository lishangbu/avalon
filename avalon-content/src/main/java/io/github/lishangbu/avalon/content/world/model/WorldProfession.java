package io.github.lishangbu.avalon.content.world.model;

/**
 * WorldCatalog 使用的类型化配置，声明id、name、description等数据。 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param id 稳定业务编号，用于查询、关联或回执引用
 * @param name 展示名称
 * @param description 玩家可读说明
 */
public record WorldProfession(String id, String name, String description) {
}
