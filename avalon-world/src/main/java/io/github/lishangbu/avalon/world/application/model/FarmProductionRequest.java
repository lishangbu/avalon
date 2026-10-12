package io.github.lishangbu.avalon.world.application.model;

import java.util.List;

/**
 * FarmProductionService 使用的命令或结果数据，包含requestId、action、plots、crop等字段。
 * 模型只表达请求或执行结果，资格、并发修订和资产结算仍由正式用例检查。
 *
 * @param requestId 本次命令的幂等请求号，重试沿用原值
 * @param action 行动代码
 * @param plots 田地声明列表
 * @param crop 作物代码
 * @param seed 服务端规则使用的随机种子
 * @param tool 工具引用
 * @param tools 工具声明或工具数量
 */
public record FarmProductionRequest(String requestId, String action, List<Integer> plots, String crop, String seed,
		String tool, int tools) {
}
