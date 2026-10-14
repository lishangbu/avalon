package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.application.command.BattleCommandService;
import io.github.lishangbu.avalon.battle.application.model.BattleViewAction;
import io.github.lishangbu.avalon.battle.application.model.BattleViewUnitView;
import io.github.lishangbu.avalon.battle.domain.BattleStateOrder;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.creature.application.query.CreatureQueryService;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/** 通过可重复野外战斗补足推荐等级，不直接发经验、改种子、削弱敌人或补写任务事实。 */
final class OpeningWildTraining {

	private OpeningWildTraining() {
	}

	/** 用正式概率削弱并捕获河岸目标；耗尽球时只从商店补购，不写入捕获或成功事实。 */
	static long capture(long owner, CharacterCommandService characters, CharacterQueryService queries,
			BattleCommandService battles, CreatureQueryService creatures) {
		var before = creatures.list(owner).stream().map(pet -> pet.id()).toList();
		for (int attempt = 0; attempt < 12; attempt++) {
			battles.recover(owner);
			if (attempt > 0) {
				move(owner, List.of("SQUARE", "SUPPLY_SHOP"), characters, queries);
				battles.purchase(owner, "SEAL_I", UUID.randomUUID().toString());
				move(owner, List.of("SQUARE", "PET_CENTER"), characters, queries);
			}
			move(owner, List.of("SQUARE", "STONE_PATH", "RIVERSIDE"), characters, queries);
			var view = battles.start(owner, "TUTORIAL", UUID.randomUUID().toString());
			for (int round = 0; round < 40 && view.status().equals("ACTIVE"); round++) {
				if (view.supplies().getOrDefault("SEAL_I", 0) == 0)
					break;
				var enemy = view.units().stream().filter(BattleViewUnitView::enemy).findFirst().orElseThrow();
				boolean weakened = enemy.hp() * 2 <= enemy.stats().health();
				var orders = view.units()
					.stream()
					.filter(unit -> !unit.enemy() && unit.hp() > 0 && (unit.hero() || unit.position() < 2))
					.map(unit -> {
						String action = unit.hero() ? weakened ? "SEAL_I" : "DEFEND" : weakened ? "DEFEND" : "水弹";
						return new BattleStateOrder(unit.id(), action,
								action.equals("DEFEND") ? unit.id() : enemy.id());
					})
					.toList();
				view = BattleTestCommands.turn(battles, owner, Long.parseLong(view.id()), view.round(), orders, false);
			}
			var captured = creatures.list(owner).stream().filter(pet -> !before.contains(pet.id())).findFirst();
			if (captured.isPresent()) {
				assertEquals("WON", view.status());
				move(owner, List.of("STONE_PATH", "SQUARE", "PET_CENTER"), characters, queries);
				return Long.parseLong(captured.get().id());
			}
			if (view.status().equals("ACTIVE"))
				BattleTestCommands.turn(battles, owner, Long.parseLong(view.id()), view.round(), List.of(), true);
			if (queries.find(owner).location().equals("RIVERSIDE"))
				move(owner, List.of("STONE_PATH", "SQUARE"), characters, queries);
			move(owner, List.of("PET_CENTER"), characters, queries);
		}
		throw new AssertionError("捕捉在测试操作预算内未成功，不能用直接发宠替代");
	}

	/** 从幻兽中心出发，每次沿相邻道路往返并免费恢复；达到目标后停留在中心。 */
	static void toLevel(long owner, int level, CharacterCommandService characters, CharacterQueryService queries,
			BattleCommandService battles) {
		for (int attempt = 0; queries.find(owner).level() < level && attempt < 250; attempt++) {
			assertEquals("PET_CENTER", queries.find(owner).location());
			battles.recover(owner);
			move(owner, List.of("SQUARE", "STONE_PATH", "RIVERSIDE"), characters, queries);
			var view = battles.start(owner, "VILLAGE", UUID.randomUUID().toString());
			for (int round = 0; view.status().equals("ACTIVE") && round < 40; round++) {
				String target = view.units()
					.stream()
					.filter(unit -> unit.enemy() && unit.hp() > 0)
					.findFirst()
					.orElseThrow()
					.id();
				var orders = view.units()
					.stream()
					.filter(unit -> !unit.enemy() && unit.hp() > 0 && (unit.hero() || unit.position() < 2))
					.map(unit -> new BattleStateOrder(unit.id(), attack(unit), target))
					.toList();
				view = BattleTestCommands.turn(battles, owner, Long.parseLong(view.id()), view.round(), orders, false);
			}
			assertEquals("WON", view.status(), "正式野外训练必须能完成：" + view.log());
			move(owner, List.of("STONE_PATH", "SQUARE", "PET_CENTER"), characters, queries);
		}
		assertTrue(queries.find(owner).level() >= level, "实际野战在预设上限内未达到推荐等级");
		battles.recover(owner);
	}

	private static String attack(BattleViewUnitView unit) {
		return unit.actions()
			.stream()
			.filter(action -> action.available() && action.mana() > 0 && action.target().equals("ENEMY"))
			.map(BattleViewAction::id)
			.findFirst()
			.orElse("ATTACK");
	}

	private static void move(long owner, List<String> route, CharacterCommandService characters,
			CharacterQueryService queries) {
		for (String location : route)
			characters.move(owner, location, queries.find(owner).revision());
	}

}
