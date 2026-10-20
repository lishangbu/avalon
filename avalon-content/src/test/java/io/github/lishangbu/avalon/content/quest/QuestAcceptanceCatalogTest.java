package io.github.lishangbu.avalon.content.quest;

import io.github.lishangbu.avalon.content.creature.CreatureCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.quest.model.QuestContentDefinition;
import io.github.lishangbu.avalon.quest.model.QuestEffect;

import io.github.lishangbu.avalon.content.quest.model.QuestContentStep;
import io.github.lishangbu.avalon.content.quest.model.QuestReception;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.content.world.model.WorldDefinition;
import io.github.lishangbu.avalon.content.world.model.WorldNpc;
import io.github.lishangbu.avalon.content.world.model.WorldNpcBinding;
import io.github.lishangbu.avalon.content.world.model.WorldPlace;
import java.util.List;
import io.github.lishangbu.avalon.quest.model.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 接取补给只允许人工交谈发物品，不能借此获得第二套经验、金币或自动到达奖励。 */
class QuestAcceptanceCatalogTest {

	private QuestContentStep step(QuestEffect effect, boolean arrival) {
		return new QuestContentStep("O008", "N026", "首次捕捉", "PET_CENTER", "登记", "完成", List.of(), true, List.of(),
				List.<String>copyOf(List.of("CAPTURE:TEST"))
					.stream()
					.map(code -> new QuestObjective(code,
							code.startsWith("ACTION:") || code.startsWith("SKILL:") ? QuestMeasure.HISTORY
									: QuestMeasure.EVENT,
							code, 1))
					.toList(),
				List.of(), (arrival ? QuestDelivery.AUTO : QuestDelivery.MANUAL), null, null, "MESHA", false,
				List.of(effect), new QuestReception("PET_CENTER", "MESHA"));
	}

	private void validate(QuestContentStep step) {
		var world = new WorldCatalog();
		// 仅保留本用例需要的接待地点；正式地图的其他任务前置由完整目录测试验证。
		var map = new WorldDefinition("PET_CENTER", world.definition().professions(),
				List.of(new WorldPlace("PET_CENTER", "伙伴中心", "接取补给", 0, 0, List.of(), null, List.of(), null,
						List.of(new WorldNpcBinding("MESHA", null, null)))),
				List.of(), List.of(new WorldNpc("MESHA", "梅莎")));
		QuestCatalog.validate(new QuestContentDefinition(List.of(step)), map, new ItemCatalog(world),
				new CreatureCatalog());
	}

	@Test
	void onlyValidManualSupplyEffectsAreAccepted() {
		var supplies = new QuestEffect("GIVE_ITEM", "SEAL_I", 5);
		assertDoesNotThrow(() -> validate(step(supplies, false)));
		assertThrows(IllegalArgumentException.class, () -> validate(step(supplies, true)));
		assertThrows(IllegalArgumentException.class, () -> validate(step(new QuestEffect("GOLD", null, 1), false)));
		assertThrows(IllegalArgumentException.class,
				() -> validate(step(new QuestEffect("GIVE_ITEM", "NOT_AN_ITEM", 1), false)));
		assertThrows(IllegalArgumentException.class,
				() -> validate(step(new QuestEffect("GIVE_ITEM", "SEAL_I", 0), false)));
	}

	@Test
	void firstCommissionStartsAtSquareAndReceptionReferencesAreValidated() {
		var world = new WorldCatalog();
		var catalog = new QuestCatalog(world, new ItemCatalog(world), new CreatureCatalog());
		var invitation = catalog.step("O001");
		assertEquals("SQUARE", invitation.acceptance().location());
		assertEquals("TERLIN", invitation.acceptance().npcId());
		assertEquals("PET_CENTER", invitation.location());
		assertTrue(catalog.acceptanceAvailable(invitation, ignored -> false));
		for (var reception : List.of(new QuestReception("MISSING_PLACE", null),
				new QuestReception("PET_CENTER", "ROWAN"))) {
			var base = step(new QuestEffect("GIVE_ITEM", "SEAL_I", 5), false);
			var invalid = new QuestContentStep(base.id(), base.quest(), base.title(), base.location(), base.label(),
					base.dialogue(), base.requires(), base.completes(), base.effects(), base.objectives(),
					base.conditions(), base.delivery(), base.branch(), base.timeout(), base.npcId(), false,
					base.acceptEffects(), reception);
			assertThrows(IllegalArgumentException.class, () -> validate(invalid));
		}
	}

	@Test
	void middleCitiesStayIndependentAndFireRequiresBothConclusions() {
		var world = new WorldCatalog();
		var catalog = new QuestCatalog(world, new ItemCatalog(world), new CreatureCatalog());
		assertEquals(List.of("N025"), catalog.step("U001").requires());
		assertEquals(List.of("N025"), catalog.step("S001").requires());
		assertEquals(java.util.Set.of("U025", "S025"), java.util.Set.copyOf(catalog.step("F001").requires()));
	}

	@Test
	void openingCaptureUsesNewEventsAndHighValueDisplayNeverConsumesPets() {
		var world = new WorldCatalog();
		var catalog = new QuestCatalog(world, new ItemCatalog(world), new CreatureCatalog());
		assertTrue(catalog.step("O008")
			.objectives()
			.stream()
			.anyMatch(
					objective -> objective.measure() == QuestMeasure.EVENT && objective.code().startsWith("CAPTURE")));
		var displays = catalog.definition()
			.steps()
			.stream()
			.filter(step -> !step.id().equals("N043"))
			.filter(step -> step.objectives()
				.stream()
				.anyMatch(objective -> objective.measure() == QuestMeasure.HOLDING
						&& objective.code().startsWith("PET:")))
			.toList();
		assertFalse(displays.isEmpty());
		for (var display : displays)
			assertTrue(display.effects().stream().noneMatch(effect -> effect.kind().equals("TAKE_PET")), display.id());
	}

	@Test
	void supportMineRemainsOptionalAndBothSourcesShareOneReward() {
		var world = new WorldCatalog();
		var catalog = new QuestCatalog(world, new ItemCatalog(world), new CreatureCatalog());
		var safe = catalog.step("F053_SAFE");
		var field = catalog.step("F053_FIELD");
		assertEquals(QuestBranchMode.SWITCHABLE, safe.branch().mode());
		assertEquals(safe.branch().group(), field.branch().group());
		assertNotEquals(safe.branch().option(), field.branch().option());
		assertEquals(QuestDelivery.AUTO, catalog.step("F053").delivery());
		for (var quest : catalog.definition().steps())
			if (!quest.quest().matches("F05[1-5]"))
				assertTrue(quest.requires().stream().noneMatch(id -> id.matches("F05[1-5].*")), quest.id());
	}

}
