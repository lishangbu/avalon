package io.github.lishangbu.avalon.world;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.command.CharacterFactCommandService;
import io.github.lishangbu.avalon.character.application.model.CharacterContext;
import io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.battle.BattleCatalog;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.content.quest.QuestRulesCatalog;
import io.github.lishangbu.avalon.content.quest.model.QuestContentDefinition;
import io.github.lishangbu.avalon.content.quest.model.QuestContentStep;
import io.github.lishangbu.avalon.content.quest.model.QuestRepeatPolicy;
import io.github.lishangbu.avalon.content.quest.model.QuestRule;
import io.github.lishangbu.avalon.content.quest.model.QuestRulesDefinition;
import io.github.lishangbu.avalon.content.quest.model.QuestType;
import io.github.lishangbu.avalon.content.world.SceneActionCatalog;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.world.application.command.QuestCommandService;
import io.github.lishangbu.avalon.world.application.model.QuestResult;
import io.github.lishangbu.avalon.world.application.query.QuestDeliveryRequirements;
import io.github.lishangbu.avalon.world.application.query.QuestObjectiveQueryService;
import io.github.lishangbu.avalon.world.application.query.QuestQueryService;
import io.github.lishangbu.avalon.world.application.query.WorldMapQueryService;
import io.github.lishangbu.avalon.world.persistence.entity.QuestReceipt;
import io.github.lishangbu.avalon.world.persistence.mapper.QuestResultCodec;
import io.github.lishangbu.avalon.world.persistence.repository.QuestParticipationRepository;
import io.github.lishangbu.avalon.world.persistence.repository.QuestRepository;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 不启动服务器和数据库，验证正式命令会拒绝离场人物，读投影与命令资格一致。 */
class NpcCommandTest {

	private final CharacterFactQueryService facts = mock(CharacterFactQueryService.class);

	private final QuestCatalog catalog = mock(QuestCatalog.class);

	private final QuestRepository receipts = mock(QuestRepository.class);

	private final QuestContentStep step = new QuestContentStep("VISIT", "BQ", "访问", "A", "交谈", "你好", List.of(), false,
			List.of(), null, List.of(), 0, false, "MILA", false, List.of(), null);

	@Test
	void absentNpcRejectsFreshCommandBeforeRewardsOrReceipt() {
		var characters = mock(CharacterCommandService.class);
		var commands = mock(CharacterFactCommandService.class);
		var inventory = mock(InventoryCommandService.class);
		var creatures = mock(CreatureCommandService.class);
		when(characters.requireOutsideDungeon(10)).thenReturn(new CharacterContext(20L, "WARRIOR", "A", 7));
		when(receipts.done(20)).thenReturn(Set.of());
		when(facts.codes(10)).thenReturn(Set.of("QUEST:MOVE"));
		when(catalog.step("VISIT")).thenReturn(step);
		when(catalog.receptionAvailable(eq(step), any())).thenReturn(false);
		var rules = mock(QuestRulesCatalog.class);
		when(rules.rule("BQ"))
			.thenReturn(new QuestRule("BQ", "访问", QuestType.MAIN, List.of(), QuestRepeatPolicy.ONCE, false));
		// 门禁失败发生在实例及奖励执行之前，宿主替身仅用于验证这条正式接口边界。
		var service = new QuestCommandService(characters, facts, catalog, receipts,
				mock(QuestDeliveryRequirements.class), mock(QuestResultCodec.class),
				mock(QuestParticipationRepository.class),
				mock(io.github.lishangbu.avalon.world.quest.QuestRuntimeService.class), rules);
		assertEquals("接待人物当前不在此处",
				assertThrows(BusinessException.class, () -> service.interact(10, "VISIT")).getMessage());
		verifyNoInteractions(commands, inventory, creatures);
		verify(receipts, never()).saveAndFlush(any());
	}

	@Test
	void questReadKeepsDirectionButDisablesAbsentReception() {
		var characters = mock(CharacterQueryService.class);
		when(characters.requireId(10)).thenReturn(20L);
		when(receipts.progress(20)).thenReturn(List.of());
		when(facts.codes(10)).thenReturn(Set.of());
		when(catalog.definition()).thenReturn(new QuestContentDefinition(List.of(step)));
		when(catalog.npcName(step)).thenReturn("米菈");
		when(catalog.acceptanceNpcName(step)).thenReturn("米菈");
		when(catalog.receptionAvailable(eq(step), any())).thenReturn(false);
		var world = mock(WorldCatalog.class);
		when(world.place(step.location()))
			.thenReturn(new WorldCatalog.Place(step.location(), "学院", "学院", 0, 0, List.of()));
		var rules = mock(QuestRulesCatalog.class);
		when(rules.definition()).thenReturn(new QuestRulesDefinition(List.of(), List.of()));
		var action = new QuestQueryService(characters, receipts, catalog, facts, mock(SceneActionCatalog.class), world,
				mock(BattleCatalog.class), rules, mock(QuestParticipationRepository.class),
				mock(QuestObjectiveQueryService.class), mock(QuestDeliveryRequirements.class))
			.state(10)
			.available()
			.getFirst();
		assertEquals("MILA", action.npcId());
		assertEquals("米菈", action.npc());
		assertFalse(action.interactable());
	}

	@Test
	void completedDialogueReplaysAfterNpcLeavesWithoutNewQualificationOrReward() {
		var characters = mock(CharacterCommandService.class);
		var commands = mock(CharacterFactCommandService.class);
		var inventory = mock(InventoryCommandService.class);
		var creatures = mock(CreatureCommandService.class);
		var codec = mock(QuestResultCodec.class);
		when(characters.requireOutsideDungeon(10)).thenReturn(new CharacterContext(20L, "WARRIOR", "B", 7));
		var receipt = new QuestReceipt();
		receipt.setResponse("saved");
		when(receipts.prior(20, "VISIT")).thenReturn(receipt);
		var result = new QuestResult("VISIT", "原来的对白", List.of());
		when(codec.decode("saved")).thenReturn(result);
		var service = new QuestCommandService(characters, facts, catalog, receipts,
				mock(QuestDeliveryRequirements.class), codec, mock(QuestParticipationRepository.class),
				mock(io.github.lishangbu.avalon.world.quest.QuestRuntimeService.class), mock(QuestRulesCatalog.class));
		assertSame(result, service.interact(10, "VISIT"));
		verifyNoInteractions(catalog, facts, commands, inventory, creatures);
		verify(receipts, never()).saveAndFlush(any());
	}

	@Test
	void mapShowsOnlyPresentBindingsAndPreservesStableNames() {
		var world = mock(WorldCatalog.class);
		var place = new WorldCatalog.Place("A", "学院", "学院", 0, 0, List.of(), null, List.of(), null,
				List.of(new WorldCatalog.NpcBinding("FEYN", null, null),
						new WorldCatalog.NpcBinding("MILA", null, "QUEST:MOVE")));
		var names = List.of(new WorldCatalog.Npc("FEYN", "费恩"), new WorldCatalog.Npc("MILA", "米菈"));
		when(world.definition())
			.thenReturn(new WorldCatalog.Definition("A", List.of(), List.of(place), List.of(), names));
		when(world.accessible(eq(place), any())).thenReturn(true);
		when(facts.codes(10)).thenReturn(Set.of("QUEST:MOVE"));
		var projection = new WorldMapQueryService(facts, world).visible(10);
		assertEquals(List.of(names.getFirst()), projection.npcs());
		assertEquals(List.of("FEYN"),
				projection.places().getFirst().npcs().stream().map(WorldCatalog.NpcBinding::npcId).toList());
	}

}
