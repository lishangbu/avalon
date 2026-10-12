package io.github.lishangbu.avalon.battle.application.model;

import java.util.List;

/**
 * AcademyTrainingQueryService 使用的独立数据投影，包含available、partners、courses等数据。
 * 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param available 当前是否满足可用条件
 * @param partners 伙伴声明列表
 * @param courses 课程声明列表
 */
public record AcademyTrainingCourses(boolean available, List<AcademyTrainingPartner> partners,
		List<AcademyTrainingCourse> courses) {
}
