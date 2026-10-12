package io.github.lishangbu.avalon.world.domain;

/**
 * FishingRules 使用的类型化规则数据，包含lastTickMillis、elapsedMillis、caughtMillis、position等字段。
 * 保持原有数据语义和构造规则；拆为顶层类型后，调用方无需依赖规则实现的内部声明。
 *
 * @param elapsedMillis 已经经过的毫秒数
 * @param position 编队位置
 * @param held 当前持有数据
 * @param status 当前业务状态
 */
public record FishingRulesState(long lastTickMillis, int elapsedMillis, int caughtMillis, double position, boolean held,
		String status) {
}
