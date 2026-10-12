package io.github.lishangbu.avalon.world.application.model;

/**
 * FishingCommandService 使用的命令或结果数据，包含requestId、fish、tool、bait等字段。
 * 模型只表达请求或执行结果，资格、并发修订和资产结算仍由正式用例检查。
 *
 * @param requestId 本次命令的幂等请求号，重试沿用原值
 * @param fish 鱼类声明或代码
 * @param tool 工具引用
 * @param bait 鱼饵声明
 */
public record FishingStart(String requestId, String fish, String tool, String bait) {
}
