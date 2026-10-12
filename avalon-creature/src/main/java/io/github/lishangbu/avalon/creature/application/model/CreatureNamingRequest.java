package io.github.lishangbu.avalon.creature.application.model;

/**
 * CreatureNamingCommandService 使用的命令或结果数据，包含requestId、name、expectedName、renamed等字段。
 * 模型只表达请求或执行结果，资格、并发修订和资产结算仍由正式用例检查。
 *
 * @param requestId 本次命令的幂等请求号，重试沿用原值
 * @param name 展示名称
 */
public record CreatureNamingRequest(String requestId, String name, String expectedName, int renamed,
		int expectedPrice) {
}
