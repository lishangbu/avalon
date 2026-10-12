package io.github.lishangbu.avalon.creature.application.model;

import java.util.List;

/**
 * LearningView 使用的独立数据投影，包含skill、minimumLevel、unavailable、books等数据。
 * 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param skill 技能代码
 * @param minimumLevel 等级下界
 * @param books 技能书声明列表
 */
public record LearningViewOption(String skill, int minimumLevel, String unavailable, List<LearningViewBook> books) {
}
