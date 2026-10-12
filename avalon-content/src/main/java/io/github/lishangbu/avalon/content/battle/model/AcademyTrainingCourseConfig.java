package io.github.lishangbu.avalon.content.battle.model;

/**
 * AcademyTrainingCatalog 使用的类型化配置，声明name、requires、goal、rounds等数据。
 * 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param name 展示名称
 * @param requires 开放前置引用
 * @param goal 目标声明
 * @param rounds 回合数量
 * @param enemyHealth 敌方生命属性
 * @param enemyAttack 敌方攻击属性
 * @param enemyDefense 敌方防御属性
 * @param enemySpeed 敌方速度属性
 * @param power 威力
 * @param objective 目标声明
 */
public record AcademyTrainingCourseConfig(String name, String requires, String goal, int rounds, int enemyHealth,
		int enemyAttack, int enemyDefense, int enemySpeed, int power, String objective) {
}
