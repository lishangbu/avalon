package io.github.lishangbu.avalon.battle.application.model;

import java.util.List;

/** 组队页面白名单。所有实例编号使用字符串，不下发账号编号、内部存档或准备修订号。 */
public record DungeonLobbyView(String id, String name, String leader, String run, String targetRun, boolean closed,
		boolean canLaunch, List<DungeonLobbyViewMember> members) {

}
