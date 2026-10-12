package io.github.lishangbu.avalon.battle.api.model;

/**
 * DungeonRunResource 使用的命令或结果数据，包含location等字段。 模型只表达请求或执行结果，资格、并发修订和资产结算仍由正式用例检查。
 *
 * @param location 地点代码
 */
public record DungeonRunMove(Integer location) {
}
