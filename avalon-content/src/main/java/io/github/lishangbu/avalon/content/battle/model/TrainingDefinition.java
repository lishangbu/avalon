package io.github.lishangbu.avalon.content.battle.model;

import java.util.Map;

/**
 * TrainingCatalog 使用的类型化配置，声明level、courses等数据。 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param level 等级
 * @param courses 课程声明列表
 */
public record TrainingDefinition(int level, Map<String, TrainingCourse> courses) {
}
