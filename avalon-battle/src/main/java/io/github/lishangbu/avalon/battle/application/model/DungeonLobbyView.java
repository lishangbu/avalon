package io.github.lishangbu.avalon.battle.application.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** 组队页面白名单。所有实例编号使用字符串，不下发账号编号、内部存档或准备修订号。 */
public record DungeonLobbyView(String id, String name, String leader, String run, String targetRun, boolean closed,
		boolean canLaunch, List<Member> members) {
	public record Member(String id, String name, String profession, int level, boolean self, boolean confirmed,
			boolean ready, boolean inBattle, Instant deadline, List<Unit> party, Map<String, Integer> supplies) {
	}

	/** 准备页只展示人物及四只拟出战伙伴；生命魔力来自实际存档而非假定满值。 */
	public record Unit(String id, String name, int level, boolean hero, int position, int health, int maxHealth,
			int mana, int maxMana) {
	}

	/** 公开招募只展示队伍规模和职业，不公开成员背包及具体资源。 */
	public record Recruitment(String id, String name, String leader, int members, int capacity,
			List<String> professions) {
	}
}
