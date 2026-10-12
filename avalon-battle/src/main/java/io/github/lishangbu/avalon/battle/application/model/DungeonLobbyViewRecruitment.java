package io.github.lishangbu.avalon.battle.application.model;

import java.util.List;

/** 公开招募只展示队伍规模和职业，不公开成员背包及具体资源。 */
public record DungeonLobbyViewRecruitment(String id, String name, String leader, int members, int capacity,
		List<String> professions) {
}
