package io.github.lishangbu.avalon.world.application.model;

import java.util.Map;

/**
 * GatheringCommandService 使用的命令或结果数据，包含requestId、node、creature、tool等字段。
 * 模型只表达请求或执行结果，资格、并发修订和资产结算仍由正式用例检查。
 *
 * @param requestId 本次命令的幂等请求号，重试沿用原值
 * @param tool 工具引用
 * @param tools 工具声明或工具数量
 * @param supplies 补给声明
 */
public record GatheringStart(String requestId, String node, String creature, String tool, int tools,
		Map<String, Integer> supplies) {
	public GatheringStart {
		supplies = supplies == null ? Map.of() : Map.copyOf(supplies);
	}

	public GatheringStart(String requestId, String node, String creature, String tool, int tools) {
		this(requestId, node, creature, tool, tools, Map.of());
	}
}
