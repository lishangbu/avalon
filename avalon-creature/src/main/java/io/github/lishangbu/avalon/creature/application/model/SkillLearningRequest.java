package io.github.lishangbu.avalon.creature.application.model;

import java.util.List;

/**
 * SkillLearningCommandService 使用的命令或结果数据，包含requestId、bookId、expectedSkills等字段。
 * 模型只表达请求或执行结果，资格、并发修订和资产结算仍由正式用例检查。
 *
 * @param requestId 本次命令的幂等请求号，重试沿用原值
 * @param bookId 已有技能书实例编号
 */
public record SkillLearningRequest(String requestId, String bookId, List<String> expectedSkills) {
}
