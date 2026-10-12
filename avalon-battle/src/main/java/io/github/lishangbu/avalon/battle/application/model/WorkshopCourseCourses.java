package io.github.lishangbu.avalon.battle.application.model;

import java.util.List;

/**
 * WorkshopCourseQueryService 使用的独立数据投影，包含active、courses等数据。
 * 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param active 是否有效或活动中
 * @param courses 课程声明列表
 */
public record WorkshopCourseCourses(WorkshopCourseView active, List<WorkshopCourseCourse> courses) {
}
