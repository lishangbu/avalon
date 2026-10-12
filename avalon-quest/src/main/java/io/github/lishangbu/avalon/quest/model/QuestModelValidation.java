package io.github.lishangbu.avalon.quest.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 模型构造共享的内部校验工具；包级可见，不扩大框架的公开接口。 */
final class QuestModelValidation {

	/** 静态工具不允许实例化。 */
	private QuestModelValidation() {
	}

	/** 编号不能为空或重复；在配置加载时失败，避免无效标识进入实例持久化。 */
	static Set<String> unique(List<String> values, String label) {
		Set<String> result = new HashSet<>();
		for (String value : values) {
			text(value, label);
			if (!result.add(value))
				throw new IllegalArgumentException(label + "重复：" + value);
		}
		return result;
	}

	/** 拒绝空标识，确保查询、事件匹配和回执来源使用明确的稳定编号。 */
	static void text(String value, String label) {
		if (value == null || value.isBlank())
			throw new IllegalArgumentException(label + "不能为空");
	}

}
