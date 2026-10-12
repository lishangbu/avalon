package io.github.lishangbu.avalon.content.life.model;

import java.util.List;

/**
 * FishingCatalog 使用的类型化配置，声明id、name、level、difficulty等数据。
 * 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param id 稳定业务编号，用于查询、关联或回执引用
 * @param name 展示名称
 * @param level 等级
 * @param difficulty 难度
 * @param points 规则使用的点数
 * @param locations 允许的地点代码
 */
public record FishingFish(String id, String name, int level, int difficulty, int points, List<String> locations) {
	public FishingFish {
		locations = List.copyOf(locations);
	}
}
