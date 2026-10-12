package io.github.lishangbu.avalon.content.life.model;

/**
 * GatheringCatalog 使用的类型化配置，声明id、name、skill、level等数据。 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param id 稳定业务编号，用于查询、关联或回执引用
 * @param name 展示名称
 * @param skill 技能代码
 * @param level 等级
 * @param location 地点代码
 * @param product 产物代码
 * @param quantity 物品或资源数量
 */
public record GatheringNode(String id, String name, String skill, int level, String location, String product,
		int quantity, String rare) {
}
