package io.github.lishangbu.avalon.world.application.model;

/**
 * SceneActionCommandService 使用的命令或结果数据，包含requestId、expectedRevision、choiceId等字段。
 * 模型只表达请求或执行结果，资格、并发修订和资产结算仍由正式用例检查。
 *
 * @param requestId 本次命令的幂等请求号，重试沿用原值
 * @param expectedRevision 客户端看见的状态修订，用于拒绝过期操作
 * @param choiceId 选择项的稳定编号
 */
public record SceneActionRequest(String requestId, long expectedRevision, String choiceId) {
}
