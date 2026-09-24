package io.github.lishangbu.avalon.content.battle;

import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.creature.CreatureCatalog;
import io.github.lishangbu.avalon.content.ContentLoader;

import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 普通副本的启动目录。战斗机制参数不进入服务代码，错误行动表在开服前拒绝。 */
@Component
public class DungeonCatalog {

	private final Definition definition;

	public DungeonCatalog(CreatureCatalog creatures, QuestCatalog quests, ItemCatalog items, WorldCatalog world) {
		var raw = ContentLoader.read("/content/dungeon.json", Definition.class);
		world.place(raw.entrance());
		if (raw.id() == null || raw.name() == null || raw.level() < 1 || raw.level() > 15 || raw.players() != 3
				|| raw.dailyStarts() < 1 || raw.roundSeconds() < 1 || raw.absenceRounds() < 1
				|| raw.confirmSeconds() < 1 || raw.holdSeconds() < 1 || raw.invitationSeconds() < 1
				|| raw.resetHour() < 0 || raw.resetHour() > 23 || quests.step(raw.requires()) == null
				|| raw.stages().isEmpty())
			throw new IllegalArgumentException("副本入口或时间配置无效");
		ZoneId.of(raw.zone());
		if (raw.supplies().isEmpty() || new HashSet<>(raw.supplies()).size() != raw.supplies().size()
				|| raw.supplies().stream().anyMatch(code -> !items.hasItem(code) || items.item(code).task()))
			throw new IllegalArgumentException("副本补给必须引用不重复的普通物品");
		if (!raw.restStops().contains(0) || new HashSet<>(raw.restStops()).size() != raw.restStops().size()
				|| raw.restStops().stream().anyMatch(stop -> stop < 0 || stop >= raw.stages().size()))
			throw new IllegalArgumentException("副本休整点必须包含入口且位于有效遭遇之间");
		var ids = new HashSet<String>();
		for (var stage : raw.stages()) {
			if (stage.id() == null || !ids.add(stage.id()) || stage.name() == null || stage.experience() < 0
					|| stage.gold() < 0)
				throw new IllegalArgumentException("副本阶段及奖励配置无效");
			if (!stage.wildSpecies().isEmpty()) {
				if (stage.healthMultiplier() <= 0 || !stage.actions().isEmpty())
					throw new IllegalArgumentException("副本普通敌人与机制行动不能混用");
				stage.wildSpecies().forEach(creatures::pet);
			}
			else {
				if (stage.health() < 1 || stage.attack() < 1 || stage.defense() < 1 || stage.speed() < 1
						|| stage.actions().isEmpty())
					throw new IllegalArgumentException("副本机制敌人属性无效");
				for (int i = 0; i < stage.actions().size(); i++) {
					var action = stage.actions().get(i);
					if (!Set.of("HEAVY", "POUNCE", "COMMAND", "BEAST", "STRIKE").contains(action.kind())
							|| action.notice() == null || action.power() < 0
							|| (action.kind().equals("BEAST") && (stage.guardHealth() < 1 || stage.guardDefense() < 1
									|| !stage.actions()
										.get(Math.floorMod(i - 1, stage.actions().size()))
										.kind()
										.equals("COMMAND"))))
						throw new IllegalArgumentException("护兽扑击必须紧接号令并配置护兽属性");
				}
			}
		}
		definition = raw;
	}

	public Definition definition() {
		return definition;
	}

	public Stage stage(String id) {
		return definition.stages()
			.stream()
			.filter(stage -> stage.id().equals(id))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("副本阶段不存在"));
	}

	public record Definition(String id, String name, int level, String requires, String entrance, int players,
			int dailyStarts, int resetHour, String zone, int roundSeconds, int absenceRounds, int confirmSeconds,
			int holdSeconds, int invitationSeconds, List<Integer> restStops, List<String> supplies,
			List<Stage> stages) {
		public Definition {
			restStops = restStops == null ? List.of() : List.copyOf(restStops);
			supplies = supplies == null ? List.of() : List.copyOf(supplies);
			stages = stages == null ? List.of() : List.copyOf(stages);
		}
	}

	public record Stage(String id, String name, List<String> wildSpecies, double healthMultiplier, int health,
			int attack, int defense, int speed, int guardHealth, int guardDefense, int experience, int gold,
			List<Action> actions) {
		public Stage {
			wildSpecies = wildSpecies == null ? List.of() : List.copyOf(wildSpecies);
			actions = actions == null ? List.of() : List.copyOf(actions);
		}
	}

	public record Action(String kind, int power, String notice) {
	}

}
