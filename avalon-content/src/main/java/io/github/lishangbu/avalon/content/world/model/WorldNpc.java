package io.github.lishangbu.avalon.content.world.model;

import java.util.List;

/** 地点内的稳定人物身份；重复中文名允许存在，不能据此合并不同人物。 */
public record WorldNpc(String id, String name, String role, String description, String greeting,
		List<String> services) {
	/** 小型规则测试可只定义人物身份；正式 JSON 必须提供完整介绍。 */
	public WorldNpc(String id, String name) {
		this(id, name, null, null, null, List.of());
	}

	public WorldNpc {
		services = services == null ? List.of() : List.copyOf(services);
	}
}
