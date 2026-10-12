package io.github.lishangbu.avalon.battle.api.model;

/**
 * DungeonLobbyResource 使用的命令或结果数据，包含run等字段。 模型只表达请求或执行结果，资格、并发修订和资产结算仍由正式用例检查。
 *
 * @param run 副本运行引用
 */
public record DungeonLobbyStarted(String run) {
}
