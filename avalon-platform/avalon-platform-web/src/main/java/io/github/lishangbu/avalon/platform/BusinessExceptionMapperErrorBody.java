package io.github.lishangbu.avalon.platform;

/**
 * BusinessExceptionMapper 使用的类型化规则数据，包含message等字段。
 * 保持原有数据语义和构造规则；拆为顶层类型后，调用方无需依赖规则实现的内部声明。
 *
 * @param message 玩家可读结果说明
 */
public record BusinessExceptionMapperErrorBody(String message) {
}
