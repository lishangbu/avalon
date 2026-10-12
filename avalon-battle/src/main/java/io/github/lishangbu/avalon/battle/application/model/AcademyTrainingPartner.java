package io.github.lishangbu.avalon.battle.application.model;

import java.util.List;

/**
 * AcademyTrainingQueryService 使用的独立数据投影，包含id、name、element、stats等数据。
 * 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param id 稳定业务编号，用于查询、关联或回执引用
 * @param name 展示名称
 * @param element 元素属性
 * @param stats 属性数据
 * @param skills 技能声明列表
 */
public record AcademyTrainingPartner(String id, String name, String element, List<Integer> stats, List<String> skills) {
}
