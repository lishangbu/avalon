package io.github.lishangbu.avalon.world.application.model;

import io.github.lishangbu.avalon.content.life.LifeSkillRulesSkill;

/**
 * LifeSkillCommandService 使用的命令或结果数据，包含requestId、skill、confirmed等字段。
 * 模型只表达请求或执行结果，资格、并发修订和资产结算仍由正式用例检查。
 *
 * @param requestId 本次命令的幂等请求号，重试沿用原值
 * @param skill 技能代码
 * @param confirmed 是否已确认
 */
public record LifeSkillRequest(String requestId, LifeSkillRulesSkill skill, boolean confirmed) {
}
