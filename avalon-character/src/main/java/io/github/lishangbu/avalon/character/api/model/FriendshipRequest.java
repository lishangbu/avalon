package io.github.lishangbu.avalon.character.api.model;

/**
 * FriendshipResource 使用的命令或结果数据，包含name等字段。 模型只表达请求或执行结果，资格、并发修订和资产结算仍由正式用例检查。
 *
 * @param name 展示名称
 */
public record FriendshipRequest(String name) {
}
