package io.github.lishangbu.avalon.battle.api.model;

import java.util.List;

/**
 * AcademyTrainingResource 使用的命令或结果数据，包含course、partners、requestId等字段。
 * 模型只表达请求或执行结果，资格、并发修订和资产结算仍由正式用例检查。
 *
 * @param course 课程引用
 * @param partners 伙伴声明列表
 * @param requestId 本次命令的幂等请求号，重试沿用原值
 */
public record AcademyTrainingStart(String course, List<String> partners, String requestId) {
}
