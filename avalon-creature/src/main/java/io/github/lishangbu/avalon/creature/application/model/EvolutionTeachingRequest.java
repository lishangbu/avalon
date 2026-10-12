package io.github.lishangbu.avalon.creature.application.model;

/**
 * EvolutionTeachingCommandService 使用的命令或结果数据，包含source、acceptBinding等字段。
 * 模型只表达请求或执行结果，资格、并发修订和资产结算仍由正式用例检查。
 *
 * @param acceptBinding 客户端是否明确接受绑定，不能替代服务端资格检查
 */
public record EvolutionTeachingRequest(String source, boolean acceptBinding) {
}
