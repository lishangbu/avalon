package io.github.lishangbu.avalon.app;

/**
 * MarketResource 使用的类型化规则数据，包含accepted等字段。 保持原有数据语义和构造规则；拆为顶层类型后，调用方无需依赖规则实现的内部声明。
 *
 * @param accepted 是否已接受
 */
public record MarketTeaching(boolean accepted) {
}
