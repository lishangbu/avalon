package io.github.lishangbu.avalon.content.life.model;

import java.util.List;

/**
 * FarmingCatalog 使用的类型化配置，声明initialPlots、maximumPlots、expansionGold、locations等数据。
 * 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param initialPlots 初始田地数量
 * @param maximumPlots 田地数量上限
 * @param locations 允许的地点代码
 * @param crops 作物声明列表
 */
public record FarmingDefinition(int initialPlots, int maximumPlots, int expansionGold, List<String> locations,
		List<FarmingCrop> crops) {
	public FarmingDefinition {
		locations = List.copyOf(locations);
		crops = List.copyOf(crops);
	}
}
