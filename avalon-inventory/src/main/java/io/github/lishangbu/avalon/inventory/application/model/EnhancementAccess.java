package io.github.lishangbu.avalon.inventory.application.model;

/**
 * EnhancementPreviewService 使用的命令或结果数据，包含available、requiredLevel等字段。
 * 模型只表达请求或执行结果，资格、并发修订和资产结算仍由正式用例检查。
 *
 * @param available 当前是否满足可用条件
 */
public record EnhancementAccess(boolean available, int requiredLevel) {
}
