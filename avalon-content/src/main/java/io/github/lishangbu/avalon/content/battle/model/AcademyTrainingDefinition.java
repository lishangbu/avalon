package io.github.lishangbu.avalon.content.battle.model;

import java.util.List;
import java.util.Map;

/**
 * AcademyTrainingCatalog 使用的类型化配置，声明location、level、partners、courses等数据。
 * 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param location 地点代码
 * @param level 等级
 * @param partners 伙伴声明列表
 * @param courses 课程声明列表
 * @param capture 捕捉声明
 */
public record AcademyTrainingDefinition(String location, int level, List<AcademyTrainingPartnerConfig> partners,
		Map<String, AcademyTrainingCourseConfig> courses, AcademyTrainingCapture capture) {
	public AcademyTrainingDefinition {
		partners = List.copyOf(partners);
		courses = Map.copyOf(courses);
	}
}
