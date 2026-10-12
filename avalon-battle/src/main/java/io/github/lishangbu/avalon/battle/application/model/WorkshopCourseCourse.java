package io.github.lishangbu.avalon.battle.application.model;

/**
 * WorkshopCourseQueryService 使用的独立数据投影，包含id、title、available、completed等数据。
 * 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param id 稳定业务编号，用于查询、关联或回执引用
 * @param title 展示标题
 * @param available 当前是否满足可用条件
 * @param completed 是否完成
 */
public record WorkshopCourseCourse(String id, String title, boolean available, boolean completed, String reason) {
}
