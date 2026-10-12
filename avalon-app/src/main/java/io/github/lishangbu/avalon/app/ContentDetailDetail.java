package io.github.lishangbu.avalon.app;

import java.util.List;

/**
 * ContentDetailResource 使用的类型化规则数据，包含name、description、rules等字段。
 * 保持原有数据语义和构造规则；拆为顶层类型后，调用方无需依赖规则实现的内部声明。
 *
 * @param name 展示名称
 * @param description 玩家可读说明
 */
public record ContentDetailDetail(String name, String description, List<String> rules) {
	public ContentDetailDetail {
		rules = List.copyOf(rules);
	}
}
