package io.github.lishangbu.avalon.content.battle.model;

/**
 * TrainingCatalog 使用的类型化配置，声明name、skill、goal、rounds等数据。
 * 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param name 展示名称
 * @param skill 技能代码
 * @param goal 目标声明
 * @param rounds 回合数量
 * @param objective 目标声明
 */
public record TrainingCourse(String name, String skill, String goal, int rounds, TrainingOpponent opponent,
		String objective) {
}
