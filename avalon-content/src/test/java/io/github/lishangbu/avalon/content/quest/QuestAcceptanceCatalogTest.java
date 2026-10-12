package io.github.lishangbu.avalon.content.quest;

import io.github.lishangbu.avalon.content.creature.CreatureCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.quest.model.QuestContentDefinition;
import io.github.lishangbu.avalon.content.quest.model.QuestContentEffect;
import io.github.lishangbu.avalon.content.quest.model.QuestContentEffectKind;
import io.github.lishangbu.avalon.content.quest.model.QuestContentStep;
import io.github.lishangbu.avalon.content.quest.model.QuestReception;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 接取补给只允许人工交谈发物品，不能借此获得第二套经验、金币或自动到达奖励。 */
class QuestAcceptanceCatalogTest {

	private QuestContentStep step(QuestContentEffect effect, boolean arrival) {
		return new QuestContentStep("BQ007", "BS01", "首次捕捉", "PET_CENTER", "登记", "完成", List.of(), true, List.of(), null,
				List.of("CAPTURE:TEST"), 0, arrival, "MESHA", false, List.of(effect), null);
	}

	private void validate(QuestContentStep step) {
		var world = new WorldCatalog();
		// 仅保留本用例需要的接待地点；正式地图的其他任务前置由完整目录测试验证。
		var map = new WorldCatalog.Definition("PET_CENTER", world.definition().professions(),
				List.of(new WorldCatalog.Place("PET_CENTER", "伙伴中心", "接取补给", 0, 0, List.of(), null, List.of(), null,
						List.of(new WorldCatalog.NpcBinding("MESHA", null, null)))),
				List.of(), List.of(new WorldCatalog.Npc("MESHA", "梅莎")));
		QuestCatalog.validate(new QuestContentDefinition(List.of(step)), map, new ItemCatalog(world),
				new CreatureCatalog());
	}

	@Test
	void portInvitationStartsWithRowanAndInvalidReceptionIsRejected() {
		var world = new WorldCatalog();
		var catalog = new QuestCatalog(world, new ItemCatalog(world), new CreatureCatalog());
		var invitation = catalog.step("BQ057");
		assertEquals("GNOME_ROWAN_SHOP", invitation.acceptance().location());
		assertEquals("ROWAN", invitation.acceptance().npcId());
		assertEquals("UNDINE_CITY", invitation.location());
		assertFalse(catalog.acceptanceAvailable(invitation, ignored -> false));
		assertTrue(catalog.acceptanceAvailable(invitation, ignored -> true));
		for (var reception : List.of(new QuestReception("MISSING_PLACE", null),
				new QuestReception("PET_CENTER", "ROWAN"))) {
			var base = step(new QuestContentEffect(QuestContentEffectKind.GIVE_ITEM, "SEAL_I", 5), false);
			var invalid = new QuestContentStep(base.id(), base.quest(), base.title(), base.location(), base.label(),
					base.dialogue(), base.requires(), base.completes(), base.effects(), base.placement(), base.facts(),
					0, false, base.npcId(), false, base.acceptEffects(), reception);
			assertThrows(IllegalArgumentException.class, () -> validate(invalid));
		}
	}

	@Test
	void onlyValidManualSupplyEffectsAreAccepted() {
		var supplies = new QuestContentEffect(QuestContentEffectKind.GIVE_ITEM, "SEAL_I", 5);
		assertDoesNotThrow(() -> validate(step(supplies, false)));
		assertThrows(IllegalArgumentException.class, () -> validate(step(supplies, true)));
		assertThrows(IllegalArgumentException.class,
				() -> validate(step(new QuestContentEffect(QuestContentEffectKind.GOLD, null, 1), false)));
		assertThrows(IllegalArgumentException.class, () -> validate(
				step(new QuestContentEffect(QuestContentEffectKind.GIVE_ITEM, "NOT_AN_ITEM", 1), false)));
		assertThrows(IllegalArgumentException.class,
				() -> validate(step(new QuestContentEffect(QuestContentEffectKind.GIVE_ITEM, "SEAL_I", 0), false)));
	}

}
