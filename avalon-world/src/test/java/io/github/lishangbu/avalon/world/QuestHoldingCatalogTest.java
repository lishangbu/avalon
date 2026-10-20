package io.github.lishangbu.avalon.world;

import io.github.lishangbu.avalon.content.creature.CreatureCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.content.quest.QuestRulesCatalog;
import io.github.lishangbu.avalon.content.quest.model.QuestContentDefinition;
import io.github.lishangbu.avalon.content.quest.model.QuestContentStep;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.quest.model.QuestMeasure;
import io.github.lishangbu.avalon.quest.model.QuestObjective;
import io.github.lishangbu.avalon.world.quest.QuestEngineCatalog;
import io.github.lishangbu.avalon.world.quest.QuestExtensions;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 使用正式目录核验持有目标引用，防止拼错物种后任务可接却永远无法完成。 */
class QuestHoldingCatalogTest {

	/** ITEM持有保持原有语义，PET持有允许所有正式物种，不限制为基础可直接捕捉形态。 */
	@Test
	void holdingAcceptsRegisteredItemsAndCreatures() {
		assertDoesNotThrow(() -> catalog("PET:STONE_GOLEM"));
		assertDoesNotThrow(() -> catalog("ITEM:HEALTH_POTION"));
		assertDoesNotThrow(() -> catalog("ITEM:STARTER_WEAPON"));
	}

	/** 前缀、空引用和未知资产在构造时失败，不能等玩家提交展示才暴露配置错误。 */
	@Test
	void holdingRejectsUnknownOrMalformedReferences() {
		for (String code : List.of("PET:", "PET:UNKNOWN", "ITEM:UNKNOWN", "CREATURE:STONE_GOLEM"))
			assertThrows(IllegalArgumentException.class, () -> catalog(code), code);
	}

	/** 只替换正式展示步骤的目标；其余世界、规则、奖励和前置继续通过真实目录校验。 */
	private QuestEngineCatalog catalog(String code) {
		var world = new WorldCatalog();
		var items = new ItemCatalog(world);
		var creatures = new CreatureCatalog();
		var original = new QuestCatalog(world, items, creatures);
		var steps = original.definition().steps().stream().map(step -> {
			if (!step.id().equals("N045"))
				return step;
			return new QuestContentStep(step.id(), step.quest(), step.title(), step.location(), step.label(),
					step.dialogue(), step.requires(), step.completes(), step.effects(),
					List.of(new QuestObjective("owned", QuestMeasure.HOLDING, code, 1)), step.conditions(),
					step.delivery(), step.branch(), step.timeout(), step.npcId(), step.environmental(),
					step.acceptEffects(), step.acceptance());
		}).toList();
		var quests = new QuestCatalog(new QuestContentDefinition(steps), world, items, creatures);
		// 扩展注册器不是本测试的关注点；实际宿主与资产效果由真实数据库集成测试覆盖。
		return new QuestEngineCatalog(quests, new QuestRulesCatalog(quests), mock(QuestExtensions.class), items,
				creatures);
	}

}
