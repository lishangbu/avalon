package io.github.lishangbu.avalon.battle.application.model;

import java.util.List;

/**
 * 当前地点可用遭遇、服务及队伍资源。服务器隐藏未满足前置的入口。
 */
public record BattleLobby(List<BattleLobbyEncounter> encounters, boolean recovery, List<BattleLobbyProduct> products,
		List<BattleLobbyVitals> vitals) {

}
