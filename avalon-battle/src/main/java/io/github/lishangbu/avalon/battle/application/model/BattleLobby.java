package io.github.lishangbu.avalon.battle.application.model;

import java.util.List;

/**
 * 当前地点可用遭遇、服务及队伍资源。服务器隐藏未满足前置的入口。
 */
public record BattleLobby(List<Encounter> encounters, boolean recovery, List<Product> products, List<Vitals> vitals) {
	public record Encounter(String id, String name, String description) {
		public Encounter(String id, String name) {
			this(id, name, null);
		}
	}

	public record Product(String code, String name, int price) {
	}

	public record Vitals(String id, String name, int hp, int maxHp, int mp, int maxMp) {
	}
}
