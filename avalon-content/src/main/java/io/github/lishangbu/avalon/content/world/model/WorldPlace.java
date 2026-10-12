package io.github.lishangbu.avalon.content.world.model;

import java.util.List;

/**
 * WorldCatalog 使用的类型化配置，声明id、name、description、x等数据。 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param id 稳定业务编号，用于查询、关联或回执引用
 * @param name 展示名称
 * @param description 玩家可读说明
 * @param x 地图横坐标
 * @param y 地图纵坐标
 * @param exits 相邻出口地点代码
 * @param requiredFact 必须满足的事实代码
 * @param services 服务声明列表
 * @param closedByFact 使入口关闭的事实代码
 * @param npcs 人物归属声明列表
 * @param requiredFacts 必须满足的事实代码集合
 */
public record WorldPlace(String id, String name, String description, int x, int y, List<String> exits,
		String requiredFact, List<String> services, String closedByFact, List<WorldNpcBinding> npcs,
		List<String> requiredFacts) {
	public WorldPlace(String id, String name, String description, int x, int y, List<String> exits, String requiredFact,
			List<String> services, String closedByFact, List<WorldNpcBinding> npcs) {
		this(id, name, description, x, y, exits, requiredFact, services, closedByFact, npcs, List.of());
	}

	public WorldPlace(String id, String name, String description, int x, int y, List<String> exits, String requiredFact,
			List<String> services, String closedByFact) {
		this(id, name, description, x, y, exits, requiredFact, services, closedByFact, List.of());
	}

	public WorldPlace(String id, String name, String description, int x, int y, List<String> exits, String requiredFact,
			List<String> services) {
		this(id, name, description, x, y, exits, requiredFact, services, null);
	}

	public WorldPlace(String id, String name, String description, int x, int y, List<String> exits) {
		this(id, name, description, x, y, exits, null, List.of());
	}

	public WorldPlace(String id, String name, String description, int x, int y, List<String> exits,
			String requiredFact) {
		this(id, name, description, x, y, exits, requiredFact, List.of());
	}

	public WorldPlace {
		exits = exits == null ? List.of() : List.copyOf(exits);
		services = services == null ? List.of() : List.copyOf(services);
		npcs = npcs == null ? List.of() : List.copyOf(npcs);
		requiredFacts = requiredFacts == null ? List.of() : List.copyOf(requiredFacts);
	}
}
