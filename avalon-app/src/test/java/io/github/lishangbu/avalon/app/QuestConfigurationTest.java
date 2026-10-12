package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.content.creature.CreatureCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.content.quest.model.QuestContentDefinition;
import io.github.lishangbu.avalon.content.quest.model.QuestContentEffect;
import io.github.lishangbu.avalon.content.quest.model.QuestContentEffectKind;
import io.github.lishangbu.avalon.content.quest.model.QuestContentStep;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 任务内容作者的错误应在启动前被定位，而不是变成玩家的半份奖励。
 */
class QuestConfigurationTest {

	final WorldCatalog worldCatalog = new WorldCatalog();

	// 自定义任务使用独立接待地点，避免正式地图中其他故事的NPC前置掩盖本用例要验证的错误。
	final WorldCatalog.Definition world = new WorldCatalog.Definition("ELDER_HOUSE",
			worldCatalog.definition().professions(),
			List.of(new WorldCatalog.Place("ELDER_HOUSE", "村长家", "测试接待地点", 0, 0, List.of(), null, List.of(), null,
					List.of(new WorldCatalog.NpcBinding("AD", null, null)))),
			List.of(), List.of(new WorldCatalog.Npc("AD", "阿德")));

	final ItemCatalog items = new ItemCatalog(worldCatalog);

	final CreatureCatalog creatures = new CreatureCatalog();

	QuestContentStep step(String id, List<String> requires, List<QuestContentEffect> effects) {
		return new QuestContentStep(id, "CUSTOM", "测试", "ELDER_HOUSE", "交谈", "对话", requires, true, effects, null,
				List.of(), 0, false, "AD", false, List.of(), null);
	}

	QuestContentDefinition definition(List<QuestContentStep> steps) {
		return new QuestContentDefinition(steps);
	}

	/**
	 * 构造独立测试步骤编号，验证目录只依赖通用前置和效果定义，不在程序中硬编码任务编号白名单。
	 */
	@Test
	void newTaskIdentifiersNeedNoSpecialCode() {
		var data = definition(List.of(step("NEW_1", List.of(), List.of()), step("NEW_2", List.of("NEW_1"),
				List.of(new QuestContentEffect(QuestContentEffectKind.EXPERIENCE, null, 12)))));
		assertSame(data, QuestCatalog.validate(data, world, items, creatures));
	}

	/**
	 * 构造不存在的前置、依赖环、缺失奖励引用及非法数量，确保内容问题在启动验证阶段暴露，不能等玩家领奖才失败。
	 */
	@Test
	void brokenReferencesCyclesAndInvalidRewardsAreRejected() {
		assertThrows(IllegalArgumentException.class, () -> QuestCatalog
			.validate(definition(List.of(step("ONE", List.of("MISSING"), List.of()))), world, items, creatures));
		assertThrows(IllegalArgumentException.class,
				() -> QuestCatalog.validate(definition(
						List.of(step("ONE", List.of("TWO"), List.of()), step("TWO", List.of("ONE"), List.of()))), world,
						items, creatures));
		assertThrows(IllegalArgumentException.class,
				() -> QuestCatalog.validate(
						definition(List.of(step("ONE", List.of(),
								List.of(new QuestContentEffect(QuestContentEffectKind.GIVE_ITEM, "MISSING", 1))))),
						world, items, creatures));
		assertThrows(IllegalArgumentException.class,
				() -> QuestCatalog.validate(
						definition(List.of(step("ONE", List.of(),
								List.of(new QuestContentEffect(QuestContentEffectKind.EXPERIENCE, null, -1))))),
						world, items, creatures));
	}

}
