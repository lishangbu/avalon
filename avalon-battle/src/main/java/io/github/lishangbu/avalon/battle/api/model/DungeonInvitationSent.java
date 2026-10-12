package io.github.lishangbu.avalon.battle.api.model;

/**
 * DungeonInvitationResource 使用的命令或结果数据，包含id等字段。 模型只表达请求或执行结果，资格、并发修订和资产结算仍由正式用例检查。
 *
 * @param id 稳定业务编号，用于查询、关联或回执引用
 */
public record DungeonInvitationSent(String id) {
}
