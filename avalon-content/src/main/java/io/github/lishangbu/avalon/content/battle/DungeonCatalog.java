package io.github.lishangbu.avalon.content.battle;

import io.github.lishangbu.avalon.content.ContentLoader;
import io.github.lishangbu.avalon.content.battle.model.DungeonDefinition;
import io.github.lishangbu.avalon.content.battle.model.DungeonStage;
import io.github.lishangbu.avalon.content.creature.CreatureCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

/** 普通副本的启动目录。战斗机制参数不进入服务代码，错误行动表在开服前拒绝。 */
@Component
public class DungeonCatalog {

	private final DungeonDefinition definition;

	@Autowired
	public DungeonCatalog(CreatureCatalog creatures, QuestCatalog quests, ItemCatalog items, WorldCatalog world) {
		this(ContentLoader.read("/content/dungeon.json", DungeonDefinition.class), creatures, quests, items, world);
	}

	/** 显式内容输入与生产资源共用同一校验，离线重放不读取当前配置。 */
	public DungeonCatalog(DungeonDefinition raw, CreatureCatalog creatures, QuestCatalog quests, ItemCatalog items,
			WorldCatalog world) {
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

	public DungeonDefinition definition() {
		return definition;
	}

	public DungeonStage stage(String id) {
		return definition.stages()
			.stream()
			.filter(stage -> stage.id().equals(id))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("副本阶段不存在"));
	}

}
