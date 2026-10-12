package io.github.lishangbu.avalon.creature.application.model;

/**
 * CreatureEvolutionCommandService 使用的命令或结果数据，包含id、species、name、bound等字段。
 * 模型只表达请求或执行结果，资格、并发修订和资产结算仍由正式用例检查。
 *
 * @param id 稳定业务编号，用于查询、关联或回执引用
 * @param species 幻兽物种代码
 * @param name 展示名称
 * @param bound 资产是否绑定
 */
public record CreatureEvolutionResult(String id, String species, String name, boolean bound) {
}
