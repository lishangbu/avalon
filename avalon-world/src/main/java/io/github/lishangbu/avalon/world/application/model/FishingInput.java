package io.github.lishangbu.avalon.world.application.model;

/**
 * FishingCommandService 使用的命令或结果数据，包含session、revision、held、foreground等字段。
 * 模型只表达请求或执行结果，资格、并发修订和资产结算仍由正式用例检查。
 *
 * @param session 当前会话引用
 * @param revision 状态修订，供后续命令并发检查
 * @param held 当前持有数据
 */
public record FishingInput(String session, long revision, boolean held, boolean foreground) {
}
