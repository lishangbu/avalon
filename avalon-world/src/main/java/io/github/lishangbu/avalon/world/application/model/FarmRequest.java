package io.github.lishangbu.avalon.world.application.model;

import java.util.List;

/**
 * FarmCommandService 使用的命令或结果数据，包含requestId、action、plots等字段。
 * 模型只表达请求或执行结果，资格、并发修订和资产结算仍由正式用例检查。
 *
 * @param requestId 本次命令的幂等请求号，重试沿用原值
 * @param action 行动代码
 * @param plots 田地声明列表
 */
public record FarmRequest(String requestId, String action, List<Integer> plots) {
}
