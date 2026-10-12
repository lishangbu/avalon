package io.github.lishangbu.avalon.battle.application.model;

/**
 * FoodCommandService 使用的命令或结果数据，包含requestId、item、target等字段。
 * 模型只表达请求或执行结果，资格、并发修订和资产结算仍由正式用例检查。
 *
 * @param requestId 本次命令的幂等请求号，重试沿用原值
 * @param item 物品代码或物品数据
 * @param target 目标编号或目标声明
 */
public record FoodRequest(String requestId, String item, String target) {
}
