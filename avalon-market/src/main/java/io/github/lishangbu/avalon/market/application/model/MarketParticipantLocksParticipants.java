package io.github.lishangbu.avalon.market.application.model;

import io.github.lishangbu.avalon.character.application.model.CharacterContext;

/**
 * MarketParticipantLocks 使用的命令或结果数据，包含seller、buyer等字段。 模型只表达请求或执行结果，资格、并发修订和资产结算仍由正式用例检查。
 *
 * @param seller 卖方上下文
 * @param buyer 买方上下文
 */
public record MarketParticipantLocksParticipants(CharacterContext seller, CharacterContext buyer) {
}
