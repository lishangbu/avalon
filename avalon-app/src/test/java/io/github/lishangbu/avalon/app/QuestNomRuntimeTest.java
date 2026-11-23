package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.content.quest.model.QuestContentDefinition;
import io.github.lishangbu.avalon.content.quest.model.QuestContentStep;
import io.github.lishangbu.avalon.content.quest.model.QuestRepeatPolicy;
import io.github.lishangbu.avalon.quest.model.QuestDelivery;
import io.github.lishangbu.avalon.quest.model.QuestStatus;
import io.github.lishangbu.avalon.world.quest.model.QuestInstanceRequest;
import io.github.lishangbu.avalon.world.quest.model.QuestOperation;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import io.github.lishangbu.avalon.quest.model.*;
import io.github.lishangbu.avalon.content.quest.model.QuestReception;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 诺姆章节的矿区、封印、遗迹及区域委托回归。 */
class QuestNomRuntimeTest extends QuestRuntimeTestSupport {

	/** 两个共振源各需新胜利，重复同一处不能替代，结算后才开放普通伙伴遭遇。 */
	@Test
	void resonanceSourcesRequireBothDistinctVictoriesBeforeRestoredBeastsAppear() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		completeOpening(owner);
		completeGate(owner, "N016");
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		transactions.run(() -> {
			facts.record(owner, "VICTORY:NOM_RESONANCE_NORTH");
			facts.record(owner, "VICTORY:NOM_RESONANCE_SOUTH");
		});
		var state = accept(owner, "N017");
		var restored = battleCatalog.encounter("NOM_RESTORED_BEASTS");
		assertFalse(encounterAccess.allowed(owner, restored));
		transactions.run(() -> {
			facts.record(owner, "KILL:STONE_GOLEM");
			facts.record(owner, "VICTORY:NOM_RESONANCE_NORTH");
			facts.record(owner, "VICTORY:NOM_RESONANCE_NORTH");
		});
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N017")));
		assertFalse(encounterAccess.allowed(owner, restored));
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_RESONANCE_SOUTH"));
		long xp = queries.find(owner).experience();
		long gold = queries.find(owner).gold();
		var delivery = request(state, QuestOperation.DELIVER, "N017");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		assertTrue(encounterAccess.allowed(owner, restored));
		runtime.command(owner, delivery);
		assertEquals(xp + 116000, queries.find(owner).experience());
		assertEquals(gold + 30, queries.find(owner).gold());
	}

	/** 物资可先于异常战斗完成，库存有效；两项都完成才开放后续突破任务。 */
	@Test
	void sealingSuppliesCanFinishInParallelAndBothTasksGateTheNextQuest() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		completeOpening(owner);
		completeGate(owner, "N016");
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		transactions.run(() -> inventoryCommands.grant(owner, "SEAL_ORE", "LIFE_ORE_0", 7, false));
		var state = accept(owner, "N018");
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N018")));
		transactions.run(() -> inventoryCommands.grant(owner, "SEAL_ORE_LAST", "LIFE_ORE_0", 1, false));
		long xp = queries.find(owner).experience();
		var delivery = request(state, QuestOperation.DELIVER, "N018");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		assertFalse(questQueries.done(owner, "N017"));
		assertTrue(inventoryQueries.list(owner).stream().noneMatch(item -> item.code().equals("LIFE_ORE_0")));
		runtime.command(owner, delivery);
		assertEquals(xp + 138000, queries.find(owner).experience());
		assertThrows(RuntimeException.class, () -> accept(owner, "N019_OUTER"));
		completeGate(owner, "N017");
		assertNotNull(accept(owner, "N019_OUTER"));
	}

	/** 两道守卫按顺序开放，中途离开不会丢外庭进度，装备及经验只结算一次。 */
	@Test
	void ruinGuardsOpenSequentiallyAndPreserveProgressAcrossReturnTrips() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		for (String gate : List.of("O013", "O014", "O015", "N016", "N017", "N018"))
			completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		var state = accept(owner, "N019_OUTER");
		var inner = battleCatalog.encounter("NOM_RUIN_INNER_GUARD");
		assertFalse(encounterAccess.allowed(owner, inner));
		jdbc.update("UPDATE player_character SET location='NOM_RESONANCE_GALLERY' WHERE id=?",
				queries.requireId(owner));
		assertThrows(RuntimeException.class,
				() -> characters.move(owner, "NOM_RUIN_INNER_COURT", queries.find(owner).revision()));
		transactions.run(() -> facts.record(owner, "KILL:STONE_GOLEM"));
		assertFalse(questQueries.done(owner, "N019_OUTER"));
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_RUIN_OUTER_GUARD"));
		assertTrue(questQueries.done(owner, "N019_OUTER"));
		assertTrue(encounterAccess.allowed(owner, inner));
		characters.move(owner, "NOM_RUIN_INNER_COURT", queries.find(owner).revision());
		characters.move(owner, "NOM_RESONANCE_GALLERY", queries.find(owner).revision());
		characters.move(owner, "NOM_RUIN_INNER_COURT", queries.find(owner).revision());
		assertTrue(questQueries.done(owner, "N019_OUTER"));
		assertFalse(questQueries.done(owner, "N019"));
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_RUIN_INNER_GUARD"));
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		long xp = queries.find(owner).experience();
		long gold = queries.find(owner).gold();
		var delivery = request(state, QuestOperation.DELIVER, "N019");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		var inventory = inventoryQueries.list(owner);
		assertEquals(1,
				inventory.stream()
					.filter(item -> item.code().equals("NOM_RUIN_BOOTS"))
					.mapToInt(item -> item.quantity())
					.sum());
		runtime.command(owner, delivery);
		assertEquals(inventory, inventoryQueries.list(owner));
		assertEquals(xp + 162000, queries.find(owner).experience());
		assertEquals(gold + 30, queries.find(owner).gold());
	}

	/** 前哨胜利与钥件同时交付才永久开门，主线封印室进度不作前置。 */
	@Test
	void relicOutpostKeyOpensIndependentOptionalDungeonEntrance() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		for (String gate : List.of("O013", "O014", "O015", "O014", "O015"))
			completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		var state = accept(owner, "N056");
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_RELIC_OUTPOST"));
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N056")));
		transactions.run(() -> inventoryCommands.grant(owner, "OUTPOST_KEY", "NOM_RELIC_KEY", 1, false));
		long gold = queries.find(owner).gold();
		var delivery = request(state, QuestOperation.DELIVER, "N056");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		runtime.command(owner, delivery);
		assertEquals(gold + 30, queries.find(owner).gold());
		assertTrue(inventoryQueries.list(owner).stream().noneMatch(item -> item.code().equals("NOM_RELIC_KEY")));
		jdbc.update("UPDATE player_character SET location='CITY_OUTSKIRTS' WHERE id=?", queries.requireId(owner));
		for (String location : List.of("GNOME_OLD_TRAIL", "GNOME_MINE_GATE", "NOM_RELIC_VAULT_ENTRANCE",
				"GNOME_MINE_GATE", "NOM_RELIC_VAULT_ENTRANCE"))
			characters.move(owner, location, queries.find(owner).revision());
		assertEquals("NOM_RELIC_VAULT_ENTRANCE", queries.find(owner).location());
		assertFalse(questQueries.done(owner, "N020"));
	}

	/** 生产点必须有接取后的指定捕捉，交付后真实开放可往返药圃。 */
	@Test
	void productionCaptureOpensRestoredHerbGardenAndRetainsPets() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		for (String gate : List.of("O013", "O014", "O015", "O014", "O015", "N054"))
			completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location='CITY_OUTSKIRTS' WHERE id=?", queries.requireId(owner));
		assertThrows(RuntimeException.class,
				() -> characters.move(owner, "NOM_RESTORED_HERB_GARDEN", queries.find(owner).revision()));
		jdbc.update("UPDATE player_character SET location='CITY_PETS' WHERE id=?", queries.requireId(owner));
		var state = accept(owner, "N055");
		transactions.run(() -> facts.record(owner, "CAPTURE:SPROUT"));
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N055")));
		transactions.run(() -> facts.record(owner, "CAPTURE_ENCOUNTER:NOM_PRODUCTION_SPROUT"));
		var pets = creatureQueries.list(owner);
		var delivery = request(state, QuestOperation.DELIVER, "N055");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		runtime.command(owner, delivery);
		assertEquals(pets, creatureQueries.list(owner));
		jdbc.update("UPDATE player_character SET location='CITY_OUTSKIRTS' WHERE id=?", queries.requireId(owner));
		characters.move(owner, "NOM_RESTORED_HERB_GARDEN", queries.find(owner).revision());
		characters.move(owner, "CITY_OUTSKIRTS", queries.find(owner).revision());
		characters.move(owner, "NOM_RESTORED_HERB_GARDEN", queries.find(owner).revision());
		assertEquals("NOM_RESTORED_HERB_GARDEN", queries.find(owner).location());
	}

	/** 人物装备教学只认可接取后的穿戴武器胜利，不消费装备。 */
	@Test
	void equipmentTrialRequiresEquippedVictoryAfterAcceptance() {
		long owner = create();
		for (String gate : List.of("O013", "O014", "O015", "O015"))
			completeGate(owner, gate);
		transactions.run(() -> facts.record(owner, "VICTORY_EQUIPPED_SLOT:WEAPON"));
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		var state = accept(owner, "N035");
		transactions.run(() -> facts.record(owner, "KILL:CLAW_CAT"));
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N035")));
		transactions.run(() -> facts.record(owner, "VICTORY_EQUIPPED_SLOT:WEAPON"));
		var before = inventoryQueries.list(owner);
		long gold = queries.find(owner).gold();
		var delivery = request(state, QuestOperation.DELIVER, "N035");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		runtime.command(owner, delivery);
		assertEquals(gold + 30, queries.find(owner).gold());
		assertEquals(before, inventoryQueries.list(owner));
	}

	/** 工程汇合后的试运行必须有现场操作和指定清理，普通击杀不算。 */
	@Test
	void workshopTrialRequiresOperationAndSpecificPatrol() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		for (String gate : List.of("O013", "O014", "O015", "O014", "N053"))
			completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		var state = accept(owner, "N054");
		transactions.run(() -> facts.record(owner, "KILL:CLAW_CAT"));
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N054")));
		jdbc.update("UPDATE player_character SET location='CITY_OUTSKIRTS' WHERE id=?", queries.requireId(owner));
		var operation = new io.github.lishangbu.avalon.world.application.model.SceneActionRequest(
				UUID.randomUUID().toString(), queries.find(owner).revision(), null);
		var result = sceneCommands.execute(owner, "N054_OPERATE", operation);
		assertEquals(result, sceneCommands.execute(owner, "N054_OPERATE", operation));
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N054")));
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_WORKSITE_PATROL"));
		long gold = queries.find(owner).gold();
		var delivery = request(state, QuestOperation.DELIVER, "N054");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		runtime.command(owner, delivery);
		assertEquals(gold + 30, queries.find(owner).gold());
	}

	/** 商道五场胜利与包裹交付同时满足，恢复供货只发一次。 */
	@Test
	void tradeRoadRequiresFivePatrolsAndParcelThenRestoresSupplyOnce() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		for (String gate : List.of("O013", "O014", "O015", "O014", "N029"))
			completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		var state = accept(owner, "N030");
		transactions.run(() -> facts.record(owner, "KILL:STONE_GOLEM"));
		for (int i = 0; i < 4; i++)
			transactions.run(() -> facts.record(owner, "VICTORY:NOM_TRADE_ROAD_PATROL"));
		characters.move(owner, "GNOME_CITY", queries.find(owner).revision());
		characters.move(owner, "CITY_OUTSKIRTS", queries.find(owner).revision());
		sceneCommands.execute(owner, "N030_RECOVER",
				new io.github.lishangbu.avalon.world.application.model.SceneActionRequest(UUID.randomUUID().toString(),
						queries.find(owner).revision(), null));
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N030")));
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_TRADE_ROAD_PATROL"));
		int potions = inventoryQueries.list(owner)
			.stream()
			.filter(item -> item.code().equals("HEALTH_POTION"))
			.mapToInt(item -> item.quantity())
			.sum();
		long gold = queries.find(owner).gold();
		var delivery = request(state, QuestOperation.DELIVER, "N030");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		runtime.command(owner, delivery);
		assertEquals(gold + 30, queries.find(owner).gold());
		assertTrue(inventoryQueries.list(owner).stream().noneMatch(item -> item.code().equals("NOM_SUPPLY_PARCEL")));
		assertEquals(potions + 2,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("HEALTH_POTION"))
					.mapToInt(item -> item.quantity())
					.sum());
	}

	/** 可选遗物委托只接受接取后的副本核心胜利，不能用普通击杀替代。 */
	@Test
	void relicGuardianRequiresNewDungeonCoreVictory() {
		long owner = create();
		for (String gate : List.of("O013", "O014", "O015", "O015", "N058", "N056"))
			completeGate(owner, gate);
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_RELIC_KEEPER"));
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		var state = accept(owner, "N059");
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N059")));
		transactions.run(() -> facts.record(owner, "KILL:STONE_GOLEM"));
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_SEAL_OUTER"));
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N059")));
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_RELIC_KEEPER"));
		long xp = queries.find(owner).experience();
		long gold = queries.find(owner).gold();
		var delivery = request(state, QuestOperation.DELIVER, "N059");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		runtime.command(owner, delivery);
		assertEquals(xp + 1200, queries.find(owner).experience());
		assertEquals(gold + 30, queries.find(owner).gold());
	}

	/** 坐标验证必须有新胜利和实际部件，交付后可按任意顺序进入两城。 */
	@Test
	void coordinateDeliveryOpensBothCityRoutesWithoutLockingAnOrder() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		for (String gate : List.of("O013", "O014", "O015", "O014", "N024"))
			completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		var state = accept(owner, "N025");
		jdbc.update("UPDATE player_character SET location='CITY_OUTSKIRTS' WHERE id=?", queries.requireId(owner));
		assertThrows(RuntimeException.class,
				() -> characters.move(owner, "GNOME_PORT_ROAD", queries.find(owner).revision()));
		assertThrows(RuntimeException.class,
				() -> characters.move(owner, "SYLPH_PASS", queries.find(owner).revision()));
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_COORDINATE_TEST"));
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N025")));
		transactions.run(() -> inventoryCommands.grant(owner, "COORDINATE_TEST_LOOT", "NOM_COORDINATE_PART", 1, false));
		long xp = queries.find(owner).experience();
		long gold = queries.find(owner).gold();
		var delivery = request(state, QuestOperation.DELIVER, "N025");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		assertTrue(inventoryQueries.list(owner).stream().noneMatch(item -> item.code().equals("NOM_COORDINATE_PART")));
		runtime.command(owner, delivery);
		assertEquals(xp + 2000, queries.find(owner).experience());
		assertEquals(gold + 50, queries.find(owner).gold());
		jdbc.update("UPDATE player_character SET location='CITY_OUTSKIRTS' WHERE id=?", queries.requireId(owner));
		for (String location : List.of("SYLPH_PASS", "SYLPH_CITY", "SYLPH_PASS", "CITY_OUTSKIRTS", "GNOME_PORT_ROAD",
				"UNDINE_CITY", "GNOME_PORT_ROAD", "CITY_OUTSKIRTS", "SYLPH_PASS", "SYLPH_CITY"))
			characters.move(owner, location, queries.find(owner).revision());
		assertEquals("SYLPH_CITY", queries.find(owner).location());
	}

	/** 返回通路清理后两件现场资产必须齐备，交付消耗且奖励不重复。 */
	@Test
	void postDungeonRecoveryRequiresClearAndBothRealSceneAssets() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		for (String gate : List.of("O013", "O014", "O015", "N019", "N023"))
			completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location='NOM_SEAL_FORECOURT' WHERE id=?", queries.requireId(owner));
		var state = accept(owner, "N024_CLEAR");
		var recordRequest = new io.github.lishangbu.avalon.world.application.model.SceneActionRequest(
				UUID.randomUUID().toString(), queries.find(owner).revision(), null);
		assertThrows(RuntimeException.class, () -> sceneCommands.execute(owner, "N024_RECORD", recordRequest));
		transactions.run(() -> facts.record(owner, "KILL:STONE_GOLEM"));
		assertFalse(questQueries.done(owner, "N024_CLEAR"));
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_RETURN_GUARD"));
		assertTrue(questQueries.done(owner, "N024_CLEAR"));
		var recovered = sceneCommands.execute(owner, "N024_RECORD", recordRequest);
		assertEquals(recovered, sceneCommands.execute(owner, "N024_RECORD", recordRequest));
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N024")));
		jdbc.update("UPDATE player_character SET location='NOM_SEAL_FORECOURT' WHERE id=?", queries.requireId(owner));
		sceneCommands.execute(owner, "N024_SHARD",
				new io.github.lishangbu.avalon.world.application.model.SceneActionRequest(UUID.randomUUID().toString(),
						queries.find(owner).revision(), null));
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		long xp = queries.find(owner).experience();
		long gold = queries.find(owner).gold();
		var delivery = request(state, QuestOperation.DELIVER, "N024");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		assertTrue(inventoryQueries.list(owner).stream().noneMatch(item -> item.code().startsWith("NOM_CORE_")));
		runtime.command(owner, delivery);
		assertEquals(xp + 1800, queries.find(owner).experience());
		assertEquals(gold + 30, queries.find(owner).gold());
	}

	/** 入本前同时登记三项委托，指定阶段按序结算，不用退出接下一项。 */
	@Test
	void dungeonAssignmentsAcceptTogetherAndCompleteOnlyTheirOrderedStages() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		completeGate(owner, "N019");
		completeGate(owner, "N020");
		jdbc.update("UPDATE player_character SET location='NOM_SEAL_FORECOURT' WHERE id=?", queries.requireId(owner));
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_SEAL_OUTER"));
		accept(owner, "N021");
		accept(owner, "N022_BRIEFING");
		accept(owner, "N023_BRIEFING");
		assertFalse(questQueries.done(owner, "N021"));
		assertFalse(questQueries.done(owner, "N022"));
		assertFalse(questQueries.done(owner, "N023"));
		long xp = queries.find(owner).experience();
		long gold = queries.find(owner).gold();
		transactions.run(() -> facts.record(owner, "KILL:STONE_GOLEM"));
		assertFalse(questQueries.done(owner, "N021"));
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_SEAL_OUTER"));
		assertTrue(questQueries.done(owner, "N021"));
		assertFalse(questQueries.done(owner, "N022"));
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_SEAL_CREATION"));
		assertTrue(questQueries.done(owner, "N022"));
		assertFalse(questQueries.done(owner, "N023"));
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_SEAL_CORE"));
		assertTrue(questQueries.done(owner, "N023"));
		assertEquals(xp + 3800, queries.find(owner).experience());
		assertEquals(gold + 90, queries.find(owner).gold());
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_SEAL_CORE"));
		assertEquals(xp + 3800, queries.find(owner).experience());
		assertEquals(gold + 90, queries.find(owner).gold());
	}

	/** 前庭需接取后清理指定哨卫，启动必须在现场且只奖励一次。 */
	@Test
	void forecourtRequiresNewVictoryBeforeLocalActivation() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		for (String gate : List.of("O013", "O014", "O015", "N019"))
			completeGate(owner, gate);
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_FORECOURT_SENTINEL"));
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		var state = accept(owner, "N020_CLEAR");
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N020")));
		jdbc.update("UPDATE player_character SET location='NOM_RUIN_INNER_COURT' WHERE id=?", queries.requireId(owner));
		characters.move(owner, "NOM_SEAL_FORECOURT", queries.find(owner).revision());
		assertFalse(questQueries.done(owner, "N020_CLEAR"));
		transactions.run(() -> facts.record(owner, "KILL:STONE_GOLEM"));
		assertFalse(questQueries.done(owner, "N020_CLEAR"));
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_FORECOURT_SENTINEL"));
		assertTrue(questQueries.done(owner, "N020_CLEAR"));
		assertFalse(questQueries.done(owner, "N020"));
		long xp = queries.find(owner).experience();
		long gold = queries.find(owner).gold();
		var delivery = request(state, QuestOperation.DELIVER, "N020");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		runtime.command(owner, delivery);
		assertEquals(xp + 188000, queries.find(owner).experience());
		assertEquals(gold + 30, queries.find(owner).gold());
	}

	/** 入口门禁只认接取后的指定守卫胜利，真正进入地下后才结算，往返不重复发奖。 */
	@Test
	void undergroundEntryRequiresNewGuardVictoryAndActualArrival() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		for (String gate : List.of("O013", "O014", "O015", "N013", "N015"))
			completeGate(owner, gate);
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_UNDERGROUND_GUARD"));
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		accept(owner, "N016_CLEAR");
		jdbc.update("UPDATE player_character SET location='NOM_OLD_DEVICE' WHERE id=?", queries.requireId(owner));
		assertThrows(RuntimeException.class,
				() -> characters.move(owner, "NOM_UNDERGROUND_ENTRY", queries.find(owner).revision()));
		transactions.run(() -> facts.record(owner, "KILL:STONE_GOLEM"));
		assertFalse(questQueries.done(owner, "N016_CLEAR"));
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_UNDERGROUND_GUARD"));
		assertTrue(questQueries.done(owner, "N016_CLEAR"));
		assertFalse(questQueries.done(owner, "N016"));
		long gold = queries.find(owner).gold();
		long xp = queries.find(owner).experience();
		characters.move(owner, "NOM_UNDERGROUND_ENTRY", queries.find(owner).revision());
		assertTrue(questQueries.done(owner, "N016"));
		characters.move(owner, "NOM_OLD_DEVICE", queries.find(owner).revision());
		characters.move(owner, "NOM_UNDERGROUND_ENTRY", queries.find(owner).revision());
		assertEquals(gold + 30, queries.find(owner).gold());
		assertEquals(xp + 95000, queries.find(owner).experience());
	}

	/** 部件安装、停用和验证顺序不可跳过，部件只扣一次，验证遭遇在停用后开放。 */
	@Test
	void deviceShutdownRequiresInstalledPartBeforeVerificationAndRewardsOnce() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		for (String gate : List.of("O013", "O014", "O015", "N013", "N014"))
			completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		var state = accept(owner, "N015_INSTALL");
		var verification = battleCatalog.encounter("NOM_DEVICE_VERIFICATION");
		assertFalse(encounterAccess.allowed(owner, verification));
		jdbc.update("UPDATE player_character SET location='NOM_OLD_DEVICE' WHERE id=?", queries.requireId(owner));
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N015_SHUTDOWN")));
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N015_INSTALL")));
		transactions.run(() -> inventoryCommands.grant(owner, "SHUTDOWN_PART", "NOM_REPLACEMENT_PART", 1, false));
		var installation = request(state, QuestOperation.DELIVER, "N015_INSTALL");
		runtime.command(owner, installation);
		assertTrue(questQueries.done(owner, "N015_INSTALL"));
		assertTrue(inventoryQueries.list(owner).stream().noneMatch(item -> item.code().equals("NOM_REPLACEMENT_PART")));
		assertFalse(encounterAccess.allowed(owner, verification));
		runtime.command(owner, request(state, QuestOperation.DELIVER, "N015_SHUTDOWN"));
		assertTrue(encounterAccess.allowed(owner, verification));
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_DEVICE_ELITE"));
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N015")));
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_DEVICE_VERIFICATION"));
		long xp = queries.find(owner).experience();
		long gold = queries.find(owner).gold();
		var delivery = request(state, QuestOperation.DELIVER, "N015");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		runtime.command(owner, delivery);
		assertEquals(xp + 76000, queries.find(owner).experience());
		assertEquals(gold + 30, queries.find(owner).gold());
	}

	/** 两场战斗之外还要真实到场；先到场、后补战斗仍需再次进入完成。 */
	@Test
	void tunnelQuestRequiresTwoNewVictoriesAndActualArrival() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		for (String gate : List.of("O013", "O014", "O015", "O014", "O015", "N004"))
			completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		transactions.run(() -> {
			facts.record(owner, "VICTORY:NOM_TUNNEL_PATROL");
			facts.record(owner, "VICTORY:NOM_TUNNEL_PATROL");
		});
		accept(owner, "N005");
		for (String place : List.of("GNOME_CITY", "CITY_OUTSKIRTS", "NOM_MINE_APPROACH", "NOM_MINE_PASSAGE"))
			characters.move(owner, place, queries.find(owner).revision());
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_TUNNEL_PATROL"));
		characters.move(owner, "NOM_ABANDONED_POST", queries.find(owner).revision());
		assertFalse(questQueries.done(owner, "N005"));
		characters.move(owner, "NOM_MINE_PASSAGE", queries.find(owner).revision());
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_TUNNEL_PATROL"));
		assertFalse(questQueries.done(owner, "N005"));
		long gold = queries.find(owner).gold();
		long xp = queries.find(owner).experience();
		characters.move(owner, "NOM_ABANDONED_POST", queries.find(owner).revision());
		assertTrue(questQueries.done(owner, "N005"));
		assertEquals(gold + 30, queries.find(owner).gold());
		assertEquals(xp + 5000, queries.find(owner).experience());
		characters.move(owner, "NOM_MINE_PASSAGE", queries.find(owner).revision());
		characters.move(owner, "NOM_ABANDONED_POST", queries.find(owner).revision());
		assertEquals(gold + 30, queries.find(owner).gold());
		assertEquals(xp + 5000, queries.find(owner).experience());
	}

	/** 哨所遗物必须在接取及胜利后现场回收，收集回执与交付均不可重复发放。 */
	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.CsvSource({ "N006,NOM_ABANDONED_POST,NOM_POST_GUARD,NOM_POST_RELIC,600,N005,1",
			"N010,NOM_INNER_SEAL,NOM_SEAL_GUARD,NOM_SEAL_FRAGMENT,17000,N009,1",
			"N011,NOM_OLD_YARD,NOM_YARD_PATROL,NOM_SHIPPING_MANIFEST,650,N010,1",
			"N012,NOM_OLD_YARD,NOM_YARD_PATROL,NOM_UNSTABLE_COMPONENT,30000,N011,2" })
	void recoveredRelicRequiresGuardVictoryAndIsConsumedOnce(String quest, String location, String guard, String item,
			int experience, String gate, int quantity) {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		for (String prerequisite : List.of("O013", "O014", "O015", "N004", gate))
			completeGate(owner, prerequisite);
		if (quest.equals("N012"))
			completeGate(owner, "N010");
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		var state = accept(owner, quest);
		jdbc.update("UPDATE player_character SET location=? WHERE id=?", location, queries.requireId(owner));
		var recovery = new io.github.lishangbu.avalon.world.application.model.SceneActionRequest(
				UUID.randomUUID().toString(), queries.find(owner).revision(), null);
		assertThrows(RuntimeException.class, () -> sceneCommands.execute(owner, quest + "_RECOVER", recovery));
		transactions.run(() -> facts.record(owner, "VICTORY:" + guard));
		var result = sceneCommands.execute(owner, quest + "_RECOVER", recovery);
		assertEquals(quantity, result.quantity());
		assertEquals(result, sceneCommands.execute(owner, quest + "_RECOVER", recovery));
		assertEquals(0,
				sceneCommands
					.execute(owner, quest + "_RECOVER",
							new io.github.lishangbu.avalon.world.application.model.SceneActionRequest(
									UUID.randomUUID().toString(), queries.find(owner).revision(), null))
					.quantity());
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		var delivery = request(state, QuestOperation.DELIVER, quest);
		long xp = queries.find(owner).experience();
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		assertTrue(inventoryQueries.list(owner).stream().noneMatch(value -> value.code().equals(item)));
		assertEquals(xp + experience, queries.find(owner).experience());
		runtime.command(owner, delivery);
		assertEquals(xp + experience, queries.find(owner).experience());
	}

	/** 两种物资齐全且现场确认才开门，拒绝时不扣料，成功后可正常进出且不重发奖励。 */
	@Test
	void stoneDoorRequiresKeyAndOreAtTheGateBeforeOpeningInnerChamber() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		for (String gate : List.of("O013", "O014", "O015", "N007", "N008"))
			completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		var state = accept(owner, "N009");
		transactions.run(() -> inventoryCommands.grant(owner, "DOOR_ORE", "LIFE_ORE_0", 4, false));
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N009")));
		jdbc.update("UPDATE player_character SET location='NOM_STONE_GATE' WHERE id=?", queries.requireId(owner));
		assertThrows(RuntimeException.class,
				() -> characters.move(owner, "NOM_INNER_SEAL", queries.find(owner).revision()));
		var before = inventoryQueries.list(owner);
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N009")));
		assertEquals(before, inventoryQueries.list(owner));
		transactions.run(() -> inventoryCommands.grant(owner, "DOOR_KEY", "NOM_STONE_KEY", 1, false));
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N009")));
		jdbc.update("UPDATE player_character SET location='NOM_STONE_GATE' WHERE id=?", queries.requireId(owner));
		long gold = queries.find(owner).gold();
		long xp = queries.find(owner).experience();
		var delivery = request(state, QuestOperation.DELIVER, "N009");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		assertTrue(inventoryQueries.list(owner)
			.stream()
			.noneMatch(item -> List.of("NOM_STONE_KEY", "LIFE_ORE_0").contains(item.code())));
		characters.move(owner, "NOM_INNER_SEAL", queries.find(owner).revision());
		assertEquals("NOM_INNER_SEAL", queries.find(owner).location());
		characters.move(owner, "NOM_STONE_GATE", queries.find(owner).revision());
		runtime.command(owner, delivery);
		assertEquals(gold + 30, queries.find(owner).gold());
		assertEquals(xp + 500, queries.find(owner).experience());
	}

	/** 既有残片可交付，矿石不能替代；仅扣三份并解锁正式首领入口。 */
	@Test
	void guardFragmentsConsumeThreeAndUnlockGatekeeper() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		completeOpening(owner);
		completeGate(owner, "N006");
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		transactions.run(() -> {
			inventoryCommands.grant(owner, "GUARD_FRAGMENTS", "NOM_GUARD_FRAGMENT", 2, false);
			inventoryCommands.grant(owner, "GUARD_WRONG_ORE", "LIFE_ORE_0", 3, false);
		});
		var state = accept(owner, "N007");
		assertFalse(encounterAccess.allowed(owner, battleCatalog.encounter("NOM_MINE_GATEKEEPER")));
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N007")));
		transactions.run(() -> inventoryCommands.grant(owner, "GUARD_FRAGMENTS_EXTRA", "NOM_GUARD_FRAGMENT", 2, false));
		long gold = queries.find(owner).gold();
		var delivery = request(state, QuestOperation.DELIVER, "N007");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		assertTrue(encounterAccess.allowed(owner, battleCatalog.encounter("NOM_MINE_GATEKEEPER")));
		assertEquals(1,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("NOM_GUARD_FRAGMENT"))
					.mapToInt(item -> item.quantity())
					.sum());
		assertEquals(3,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("LIFE_ORE_0"))
					.mapToInt(item -> item.quantity())
					.sum());
		assertEquals(gold + 30, queries.find(owner).gold());
		runtime.command(owner, delivery);
		assertEquals(gold + 30, queries.find(owner).gold());
	}

	/** 开篇战斗目标排除历史与异地事件，并按实际数量发放一次明确经验奖励。 */
	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.CsvSource({ "N001,VICTORY:NOM_MINE_PATROL,1,3300,O015",
			"N002,KILL_LOCATION:NOM_MINE_APPROACH,6,5000,N001", "N013,VICTORY:NOM_TRANSPORT_CLEAR,3,102000,N012" })
	void openingMineCombatRequiresNewCorrectEventsAndAwardsExperienceOnce(String quest, String event, int amount,
			int experience, String gate) {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		completeOpening(owner);
		completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		transactions.run(() -> {
			for (int i = 0; i < amount; i++)
				facts.record(owner, event);
		});
		var state = accept(owner, quest);
		transactions.run(() -> {
			for (int i = 0; i < amount; i++) {
				facts.record(owner, "KILL:STONE_GOLEM");
				facts.record(owner, "KILL_LOCATION:CITY_OUTSKIRTS");
			}
			for (int i = 0; i < amount - 1; i++)
				facts.record(owner, event);
		});
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, quest)));
		long xp = queries.find(owner).experience();
		long gold = queries.find(owner).gold();
		transactions.run(() -> facts.record(owner, event));
		var delivery = request(state, QuestOperation.DELIVER, quest);
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		assertEquals(xp + experience, queries.find(owner).experience());
		assertEquals(gold + 30, queries.find(owner).gold());
		runtime.command(owner, delivery);
		assertEquals(xp + experience, queries.find(owner).experience());
		assertEquals(gold + 30, queries.find(owner).gold());
	}

	/** 支撑任务与兽群清理并行，既有矿石有效；不足不扣、补齐扣六份且不重复发奖。 */
	@Test
	void mineSupportsCanBeDeliveredBeforeBeastClearance() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		completeOpening(owner);
		completeGate(owner, "N001");
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		transactions.run(() -> inventoryCommands.grant(owner, "SUPPORT_ORE", "LIFE_ORE_0", 5, false));
		var state = accept(owner, "N003");
		assertFalse(questQueries.done(owner, "N002"));
		var before = inventoryQueries.list(owner);
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N003")));
		assertEquals(before, inventoryQueries.list(owner));
		transactions.run(() -> inventoryCommands.grant(owner, "SUPPORT_ORE_LAST", "LIFE_ORE_0", 1, false));
		long gold = queries.find(owner).gold();
		var delivery = request(state, QuestOperation.DELIVER, "N003");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		assertTrue(inventoryQueries.list(owner).stream().noneMatch(item -> item.code().equals("LIFE_ORE_0")));
		assertEquals(gold + 30, queries.find(owner).gold());
		runtime.command(owner, delivery);
		assertEquals(gold + 30, queries.find(owner).gold());
	}

	/** 正式主线OR要先满足两项前置，完成前切换、任一路完成后汇合且只收所选材料。 */
	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.ValueSource(strings = { "FIGHT", "REPAIR" })
	void mineRouteSupportsBothMethodsAndJoinsIntoNextQuest(String option) {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		completeOpening(owner);
		completeGate(owner, "N003");
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		assertThrows(RuntimeException.class, () -> accept(owner, "N004_FIGHT"));
		completeGate(owner, "N002");
		transactions.run(() -> inventoryCommands.grant(owner, "ROUTE_WOOD", "LIFE_WOOD_0", 4, false));
		var state = accept(owner, "N004_FIGHT");
		for (String selected : List.of(option.equals("FIGHT") ? "REPAIR" : "FIGHT", option))
			runtime.command(owner, new QuestInstanceRequest(UUID.randomUUID().toString(), state.id(), state.round(),
					QuestOperation.CHOOSE, null, "NOM_MINE_ROUTE", selected));
		long gold = queries.find(owner).gold();
		if (option.equals("FIGHT")) {
			transactions.run(() -> facts.record(owner, "KILL:STONE_GOLEM"));
			assertThrows(RuntimeException.class,
					() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N004_FIGHT")));
			transactions.run(() -> facts.record(owner, "VICTORY:NOM_OLD_ROAD_GUARD"));
		}
		var delivery = request(state, QuestOperation.DELIVER, "N004_" + option);
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		assertTrue(questQueries.done(owner, "N004"));
		assertEquals(option.equals("FIGHT") ? 4 : 0,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("LIFE_WOOD_0"))
					.mapToInt(item -> item.quantity())
					.sum());
		assertEquals(gold + 30, queries.find(owner).gold());
		runtime.command(owner, delivery);
		assertEquals(gold + 30, queries.find(owner).gold());
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner,
						new QuestInstanceRequest(UUID.randomUUID().toString(), state.id(), state.round(),
								QuestOperation.CHOOSE, null, "NOM_MINE_ROUTE",
								option.equals("FIGHT") ? "REPAIR" : "FIGHT")));
		assertNotNull(accept(owner, "N005"));
	}

	/** 矿场入口要求N007，支持沿正式道路往返。 */
	@Test
	void stoneGateRouteOpensAfterFragmentsWithoutLegacyMineGate() {
		long owner = create();
		completeGate(owner, "O013");
		completeGate(owner, "O015");
		jdbc.update("UPDATE player_character SET location='CITY_OUTSKIRTS' WHERE id=?", queries.requireId(owner));
		assertThrows(RuntimeException.class,
				() -> characters.move(owner, "NOM_STONE_GATE", queries.find(owner).revision()));
		completeGate(owner, "N007");
		characters.move(owner, "NOM_STONE_GATE", queries.find(owner).revision());
		assertEquals("NOM_STONE_GATE", queries.find(owner).location());
		characters.move(owner, "CITY_OUTSKIRTS", queries.find(owner).revision());
		assertEquals("CITY_OUTSKIRTS", queries.find(owner).location());
		openingMove(owner, "GNOME_OLD_TRAIL", "GNOME_MINE_GATE", "NOM_DEEP_CAVERN", "NOM_STONE_GATE", "CITY_OUTSKIRTS");
		assertTrue(encounterAccess.allowed(owner, battleCatalog.encounter("NOM_CAVERN_GOLEM")));
	}

	/** 两项正式首领委托要求新胜利，人物装备与金币只发放一次。 */
	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.CsvSource({ "N049,NOM_WATCHTOWER_BOSS,NOM_TOWER_RELIC,O015",
			"N008,NOM_MINE_GATEKEEPER,NOM_MINER_COAT,N007", "N014,NOM_DEVICE_ELITE,NOM_FIELD_HELM,N013" })
	void bossQuestRequiresNewVictoryAndAwardsHumanEquipmentOnce(String quest, String encounter, String item,
			String gate) {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		completeOpening(owner);
		completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location='CITY_SHOP' WHERE id=?", queries.requireId(owner));
		transactions.run(() -> facts.record(owner, "VICTORY:" + encounter));
		var state = accept(owner, quest);
		transactions.run(() -> facts.record(owner, "KILL:STONE_GOLEM"));
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, quest)));
		transactions.run(() -> facts.record(owner, "VICTORY:" + encounter));
		var delivery = request(state, QuestOperation.DELIVER, quest);
		long gold = queries.find(owner).gold();
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		assertEquals(1, inventoryQueries.list(owner).stream().filter(value -> value.code().equals(item)).count());
		assertEquals(gold + 30, queries.find(owner).gold());
		var equipment = inventoryQueries.list(owner);
		runtime.command(owner, delivery);
		assertEquals(equipment, inventoryQueries.list(owner));
		assertEquals(gold + 30, queries.find(owner).gold());
	}

	/** 正式三物种收藏要求同时持有；重复同种不能抵另一种，结算和重放均不收走伙伴。 */
	@Test
	void plainsCollectionRequiresAllThreeOwnedSpeciesWithoutConsumingThem() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		completeGate(owner, "O015");
		completeOpening(owner);
		jdbc.update("UPDATE player_character SET location='CITY_PETS' WHERE id=?", queries.requireId(owner));
		transactions.run(() -> {
			creatures.grant(owner, "COLLECTION_CAT", "CLAW_CAT");
			creatures.grant(owner, "COLLECTION_CAT_AGAIN", "CLAW_CAT");
			creatures.grant(owner, "COLLECTION_SPROUT", "SPROUT");
		});
		var state = accept(owner, "N040");
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N040")));
		transactions.run(() -> creatures.grant(owner, "COLLECTION_LIZARD", "THUNDER_LIZARD"));
		var before = creatureQueries.list(owner);
		var delivery = request(state, QuestOperation.DELIVER, "N040");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		assertEquals(before, creatureQueries.list(owner));
		runtime.command(owner, delivery);
		assertEquals(before, creatureQueries.list(owner));
	}

	/** 正式两步任务与现场门禁：清理后开放指定普通遭遇，其他来源捕捉不算。 */
	@Test
	void deepCavernCaptureOpensOnlyAfterEliteAndRequiresItsOwnEncounter() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		// 为隔离两步捕捉规则补建新主线前置回执，目标和解锁均使用正式内容。
		completeGate(owner, "N007");
		completeOpening(owner);
		jdbc.update("UPDATE player_character SET location='NOM_DEEP_CAVERN' WHERE id=?", queries.requireId(owner));
		var moth = battleCatalog.encounter("NOM_CAVERN_MOTH");
		assertFalse(encounterAccess.allowed(owner, moth));
		var state = accept(owner, "N044_CLEAR");
		transactions.run(() -> facts.record(owner, "KILL:STONE_GOLEM"));
		assertFalse(questQueries.done(owner, "N044_CLEAR"));
		assertFalse(encounterAccess.allowed(owner, moth));
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_CAVERN_ELITE"));
		assertTrue(questQueries.done(owner, "N044_CLEAR"));
		assertTrue(encounterAccess.allowed(owner, moth));
		jdbc.update("UPDATE player_character SET location='CITY_PETS' WHERE id=?", queries.requireId(owner));
		transactions.run(() -> facts.record(owner, "CAPTURE:MOONLIGHT_MOTH"));
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N044")));
		transactions.run(() -> facts.record(owner, "CAPTURE_ENCOUNTER:NOM_CAVERN_MOTH"));
		var delivery = request(state, QuestOperation.DELIVER, "N044");
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		assertEquals(3,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("SEAL_II"))
					.mapToInt(item -> item.quantity())
					.sum());
		var settled = inventoryQueries.list(owner);
		runtime.command(owner, delivery);
		assertEquals(settled, inventoryQueries.list(owner));
	}

	/** 使用正式任务资产和正式现场来源，验证先接取、到场回收、不可刷取及最终扣除。 */
	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.ValueSource(strings = { "N048", "N051" })
	void recoveredTaskAssetsRequireAcceptanceAndAreDeliveredOnce(String id) {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		var content = catalog.step(id);
		configure(id, QuestRepeatPolicy.ONCE, 1, 1, null);
		var base = catalog.step(id);
		var step = new QuestContentStep(id, id, content.title(), base.location(), content.label(), content.dialogue(),
				List.of(), true, content.effects(), content.objectives(), List.of(), QuestDelivery.MANUAL, null, null,
				null, true, List.of(), new QuestReception(base.location(), null));
		var all = new ArrayList<>(catalog.definition().steps());
		all.removeIf(value -> value.id().equals(id));
		all.add(step);
		doReturn(new QuestContentDefinition(all)).when(catalog).definition();
		doReturn(step).when(catalog).step(id);
		long owner = create();
		String action = id + "_RECOVER";
		// 隔离回收事务，区域使用完整新开篇前置；不跳过接取与现场资产校验。
		completeOpening(owner);
		jdbc.update("UPDATE player_character SET location='CITY_OUTSKIRTS' WHERE id=?", queries.requireId(owner));
		assertThrows(RuntimeException.class,
				() -> sceneCommands.execute(owner, action,
						new io.github.lishangbu.avalon.world.application.model.SceneActionRequest(
								UUID.randomUUID().toString(), queries.find(owner).revision(), null)));
		jdbc.update("UPDATE player_character SET location=? WHERE id=?", base.location(), queries.requireId(owner));
		var state = accept(owner, id);
		assertThrows(RuntimeException.class, () -> runtime.command(owner, request(state, QuestOperation.DELIVER, id)));
		jdbc.update("UPDATE player_character SET location='CITY_OUTSKIRTS' WHERE id=?", queries.requireId(owner));
		var recovery = new io.github.lishangbu.avalon.world.application.model.SceneActionRequest(
				UUID.randomUUID().toString(), queries.find(owner).revision(), null);
		var result = sceneCommands.execute(owner, action, recovery);
		assertEquals(id.equals("N048") ? 1 : 4, result.quantity());
		var recovered = inventoryQueries.list(owner);
		assertEquals(result, sceneCommands.execute(owner, action, recovery));
		assertEquals(0,
				sceneCommands
					.execute(owner, action,
							new io.github.lishangbu.avalon.world.application.model.SceneActionRequest(
									UUID.randomUUID().toString(), queries.find(owner).revision(), null))
					.quantity());
		assertEquals(recovered, inventoryQueries.list(owner));
		jdbc.update("UPDATE player_character SET location=? WHERE id=?", base.location(), queries.requireId(owner));
		long gold = queries.find(owner).gold();
		var delivery = request(state, QuestOperation.DELIVER, id);
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		assertTrue(inventoryQueries.list(owner).stream().noneMatch(item -> item.code().equals(result.itemCode())));
		assertEquals(gold + 30, queries.find(owner).gold());
		runtime.command(owner, delivery);
		assertEquals(gold + 30, queries.find(owner).gold());
	}

}
