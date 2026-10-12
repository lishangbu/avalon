package io.github.lishangbu.avalon.world.application.model;

import java.util.Map;

/**
 * LifeCraftingService 使用的命令或结果数据，包含requestId、recipe、quantity、tool等字段。
 * 模型只表达请求或执行结果，资格、并发修订和资产结算仍由正式用例检查。
 *
 * @param requestId 本次命令的幂等请求号，重试沿用原值
 * @param recipe 配方代码
 * @param quantity 物品或资源数量
 * @param tool 工具引用
 * @param tools 工具声明或工具数量
 * @param materials 材料需求或材料投影
 */
public record LifeCraftingRequest(String requestId, String recipe, int quantity, String tool, int tools,
		Map<String, Integer> materials) {
}
