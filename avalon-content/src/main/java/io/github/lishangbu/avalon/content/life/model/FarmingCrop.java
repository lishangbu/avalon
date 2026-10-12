package io.github.lishangbu.avalon.content.life.model;

/**
 * FarmingCatalog 使用的类型化配置，声明id、name、seed、product等数据。 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param id 稳定业务编号，用于查询、关联或回执引用
 * @param name 展示名称
 * @param seed 服务端规则使用的随机种子
 * @param product 产物代码
 * @param level 等级
 * @param growthSeconds 生长时长秒数
 * @param quantity 物品或资源数量
 * @param points 规则使用的点数
 */
public record FarmingCrop(String id, String name, String seed, String product, int level, int growthSeconds,
		int quantity, int points) {
}
