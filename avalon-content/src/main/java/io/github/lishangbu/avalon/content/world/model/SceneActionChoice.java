package io.github.lishangbu.avalon.content.world.model;

/**
 * SceneActionCatalog 使用的类型化配置，声明id、label等数据。 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param id 稳定业务编号，用于查询、关联或回执引用
 * @param label 操作展示文字
 */
public record SceneActionChoice(String id, String label) {
}
