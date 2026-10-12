package io.github.lishangbu.avalon.creature.application.model;

/**
 * ResearchExchangeCommandService 使用的命令或结果数据，包含requestId、code、quantity、unitPrice等字段。
 * 模型只表达请求或执行结果，资格、并发修订和资产结算仍由正式用例检查。
 *
 * @param requestId 本次命令的幂等请求号，重试沿用原值
 * @param code 内容目录中的稳定代码
 * @param quantity 物品或资源数量
 * @param unitPrice 单价
 * @param acceptBinding 客户端是否明确接受绑定，不能替代服务端资格检查
 */
public record ResearchExchangeRequest(String requestId, String code, int quantity, int unitPrice,
		boolean acceptBinding) {
}
