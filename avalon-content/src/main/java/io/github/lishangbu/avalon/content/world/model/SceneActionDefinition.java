package io.github.lishangbu.avalon.content.world.model;

import java.util.List;

/**
 * SceneActionCatalog 使用的类型化配置，声明actions、information等数据。
 * 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param actions 允许的行动声明
 */
public record SceneActionDefinition(List<SceneAction> actions, List<SceneActionInformation> information) {
	public SceneActionDefinition(List<SceneAction> actions) {
		this(actions, List.of());
	}

	public SceneActionDefinition {
		actions = actions == null ? List.of() : List.copyOf(actions);
		information = information == null ? List.of() : List.copyOf(information);
	}
}
