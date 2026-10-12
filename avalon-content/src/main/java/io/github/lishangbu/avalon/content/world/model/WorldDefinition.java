package io.github.lishangbu.avalon.content.world.model;

import java.util.List;

/**
 * WorldCatalog 使用的类型化配置，声明start、professions、places、safeReturns等数据。
 * 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param places 地点声明列表
 * @param npcs 人物归属声明列表
 */
public record WorldDefinition(String start, List<WorldProfession> professions, List<WorldPlace> places,
		List<WorldSafeReturn> safeReturns, List<WorldNpc> npcs) {
	public WorldDefinition(String start, List<WorldProfession> professions, List<WorldPlace> places,
			List<WorldSafeReturn> safeReturns) {
		this(start, professions, places, safeReturns, List.of());
	}

	public WorldDefinition(String start, List<WorldProfession> professions, List<WorldPlace> places) {
		this(start, professions, places, List.of());
	}

	public WorldDefinition {
		professions = professions == null ? List.of() : List.copyOf(professions);
		places = places == null ? List.of() : List.copyOf(places);
		safeReturns = safeReturns == null ? List.of() : List.copyOf(safeReturns);
		npcs = npcs == null ? List.of() : List.copyOf(npcs);
	}
}
