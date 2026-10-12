package io.github.lishangbu.avalon.battle.domain;

/**
 * BattleState 使用的类型化规则数据，包含owner、code、quantity等字段。
 * 保持原有数据语义和构造规则；拆为顶层类型后，调用方无需依赖规则实现的内部声明。
 *
 * @param owner 业务主体账号编号，不能作为客户端授权依据
 * @param code 内容目录中的稳定代码
 * @param quantity 物品或资源数量
 */
public record BattleStateLoot(long owner, String code, int quantity) {
}
