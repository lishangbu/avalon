package io.github.lishangbu.avalon.world.persistence.repository;

/**
 * LifeOperationRepository 使用的类型化规则数据，包含payload、result等字段。
 * 保持原有数据语义和构造规则；拆为顶层类型后，调用方无需依赖规则实现的内部声明。
 *
 * @param payload 规范化命令载荷
 */
public record LifeOperationRepositoryReceipt(String payload, String result) {
}
