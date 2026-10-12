package io.github.lishangbu.avalon.battle.application.model;

import java.util.List;
import java.util.Map;

/** 战间公开进度；节点编号仅用于路线移动，不暴露内部账号与历史提交凭证。 */
public record DungeonRunView(String id, String name, String leader, String self, int cleared, boolean completed,
		String battle, String previousBattle, boolean canBegin, boolean canReady, boolean canRest, boolean recruiting,
		boolean canRecruit, List<Integer> destinations, List<DungeonRunViewNode> route,
		List<DungeonRunViewMember> members, Map<String, Integer> supplies) {

}
