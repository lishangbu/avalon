package io.github.lishangbu.avalon.creature.domain;

import java.util.List;

/** 返回本次新增、确实遗忘和最终持有的技能，供事务持久化及结果页面使用。 */
public record SkillLearningRulesResult(String learned, List<String> forgotten, List<String> remaining) {
}
