package io.github.lishangbu.avalon.battle.application.model;

/**
 * WorkshopCourseCommandService 使用的命令或结果数据，包含requestId、revision、type、target等字段。
 * 模型只表达请求或执行结果，资格、并发修订和资产结算仍由正式用例检查。
 *
 * @param requestId 本次命令的幂等请求号，重试沿用原值
 * @param revision 状态修订，供后续命令并发检查
 * @param type 业务分类
 * @param target 目标编号或目标声明
 * @param position 编队位置
 */
public record WorkshopCourseAction(String requestId, long revision, String type, String target, Integer position) {
}
