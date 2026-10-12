package io.github.lishangbu.avalon.content.battle.model;

import java.util.List;

/**
 * DungeonCatalog 使用的类型化配置，声明id、name、level、requires等数据。
 * 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param id 稳定业务编号，用于查询、关联或回执引用
 * @param name 展示名称
 * @param level 等级
 * @param requires 开放前置引用
 * @param entrance 副本入口地点
 * @param players 角色投影列表
 * @param dailyStarts 每日开启次数
 * @param resetHour 每日重置小时
 * @param roundSeconds 回合时长秒数
 * @param restStops 恢复地点列表
 * @param supplies 补给声明
 * @param stages 阶段声明列表
 */
public record DungeonDefinition(String id, String name, int level, String requires, String entrance, int players,
		int dailyStarts, int resetHour, String zone, int roundSeconds, int absenceRounds, int confirmSeconds,
		int holdSeconds, int invitationSeconds, List<Integer> restStops, List<String> supplies,
		List<DungeonStage> stages) {
	public DungeonDefinition {
		restStops = restStops == null ? List.of() : List.copyOf(restStops);
		supplies = supplies == null ? List.of() : List.copyOf(supplies);
		stages = stages == null ? List.of() : List.copyOf(stages);
	}
}
