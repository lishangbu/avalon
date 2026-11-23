package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.content.quest.model.QuestContentDefinition;
import io.github.lishangbu.avalon.quest.model.QuestEffect;
import io.github.lishangbu.avalon.content.quest.model.QuestContentStep;
import io.github.lishangbu.avalon.content.quest.model.QuestRepeatPolicy;
import io.github.lishangbu.avalon.quest.model.QuestBranch;
import io.github.lishangbu.avalon.quest.model.QuestDelivery;
import io.github.lishangbu.avalon.quest.model.QuestMeasure;
import io.github.lishangbu.avalon.quest.model.QuestObjective;
import io.github.lishangbu.avalon.quest.model.QuestStatus;
import io.github.lishangbu.avalon.quest.model.QuestTimeout;
import io.github.lishangbu.avalon.world.application.model.QuestAcceptRequest;
import io.github.lishangbu.avalon.world.quest.model.QuestInstanceRequest;
import io.github.lishangbu.avalon.world.quest.model.QuestOperation;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import io.github.lishangbu.avalon.quest.model.*;
import io.github.lishangbu.avalon.content.quest.model.QuestReception;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 任务运行时的归属、交付、重放、配额、分支与事务回滚契约。 */
class QuestRuntimeContractTest extends QuestRuntimeTestSupport {

	@Test
	void selectedPetDeliveryRejectsMissingProtectedChangedAndForeignIndividualsAndReplaysOnce() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		long owner = create();
		transactions.run(() -> characters.grantExperience(owner, 7380));
		for (String gate : List.of("O014", "O015"))
			completeGate(owner, gate);
		jdbc.update("UPDATE player_character SET location=? WHERE id=?", "CITY_PETS", queries.requireId(owner));
		transactions.run(() -> {
			creatures.grant(owner, "KEEPER", "CLAW_CAT");
			creatures.grant(owner, "HAND_IN", "STONE_GOLEM");
			creatures.grant(owner, "KEEP_GOLEM", "STONE_GOLEM");
		});
		var golems = creatureQueries.list(owner).stream().filter(pet -> pet.species().equals("STONE_GOLEM")).toList();
		String selectedId = golems.getFirst().id();
		jdbc.update("UPDATE owned_creature SET party_position=100 WHERE id=?", Long.parseLong(selectedId));
		var state = acceptOpening(owner, "N043");
		long gold = queries.find(owner).gold();
		long other = create();
		transactions.run(() -> creatures.grant(other, "FOREIGN", "STONE_GOLEM"));
		var foreign = creatureQueries.list(other).getFirst();
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, new QuestInstanceRequest(UUID.randomUUID().toString(), state.id(),
						state.round(), QuestOperation.DELIVER, "N043", null, null, List.of(foreign))));
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, "N043")));
		var selected = creatureQueries.list(owner)
			.stream()
			.filter(pet -> pet.id().equals(selectedId))
			.findFirst()
			.orElseThrow();
		var delivery = new QuestInstanceRequest(UUID.randomUUID().toString(), state.id(), state.round(),
				QuestOperation.DELIVER, "N043", null, null, List.of(selected));
		jdbc.update("UPDATE owned_creature SET party_position=3 WHERE id=?", Long.parseLong(selectedId));
		var reserve = creatureQueries.list(owner)
			.stream()
			.filter(pet -> pet.id().equals(selectedId))
			.findFirst()
			.orElseThrow();
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, new QuestInstanceRequest(UUID.randomUUID().toString(), state.id(),
						state.round(), QuestOperation.DELIVER, "N043", null, null, List.of(reserve))));
		jdbc.update("UPDATE owned_creature SET party_position=100 WHERE id=?", Long.parseLong(selectedId));
		jdbc.update("UPDATE owned_creature SET locked=true WHERE id=?", Long.parseLong(selectedId));
		assertThrows(RuntimeException.class, () -> runtime.command(owner, delivery));
		jdbc.update("UPDATE owned_creature SET locked=false, level=level+1 WHERE id=?", Long.parseLong(selectedId));
		assertThrows(RuntimeException.class, () -> runtime.command(owner, delivery));
		jdbc.update("UPDATE owned_creature SET level=level-1 WHERE id=?", Long.parseLong(selectedId));
		assertEquals(gold, queries.find(owner).gold());
		// 在已选伙伴归档后的金币结算阶段失败，整个事务必须恢复伙伴、进度和请求号。
		jdbc.update("UPDATE player_character SET gold=? WHERE id=?", Long.MAX_VALUE, queries.requireId(owner));
		assertThrows(ArithmeticException.class, () -> runtime.command(owner, delivery));
		assertTrue(creatureQueries.list(owner).stream().anyMatch(pet -> pet.id().equals(selectedId)));
		assertNotEquals(QuestStatus.COMPLETED, runtime.latest(queries.requireId(owner), "N043").status());
		jdbc.update("UPDATE player_character SET gold=? WHERE id=?", gold, queries.requireId(owner));
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		runtime.command(owner, delivery);
		assertEquals(gold + 60, queries.find(owner).gold());
		assertFalse(creatureQueries.list(owner).stream().anyMatch(pet -> pet.id().equals(selectedId)));
		assertTrue(creatureQueries.list(owner).stream().anyMatch(pet -> pet.id().equals(golems.getLast().id())));
		var different = new QuestInstanceRequest(delivery.requestId(), state.id(), state.round(),
				QuestOperation.DELIVER, "N043", null, null, List.of(golems.getLast()));
		assertThrows(RuntimeException.class, () -> runtime.command(owner, different));
	}

	/** 指定精英只认接取后的指定遭遇胜利，不认历史胜利或同物种普通击杀。 */
	@Test
	void tradeRoadEliteRequiresNewDesignatedVictory() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		var content = catalog.step("N029");
		String id = "TEST_TRADE_ROAD_ELITE";
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
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_TRADE_ROAD_ELITE"));
		var state = accept(owner, id);
		assertThrows(RuntimeException.class, () -> runtime.command(owner, request(state, QuestOperation.DELIVER, id)));
		transactions.run(() -> {
			facts.record(owner, "KILL:CLAW_CAT");
			facts.record(owner, "VICTORY:PLAINS");
		});
		assertThrows(RuntimeException.class, () -> runtime.command(owner, request(state, QuestOperation.DELIVER, id)));
		long gold = queries.find(owner).gold();
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_TRADE_ROAD_ELITE"));
		var delivery = request(state, QuestOperation.DELIVER, id);
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		assertEquals(gold + 30, queries.find(owner).gold());
		assertEquals(1,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("JOURNEY_HANDS"))
					.mapToInt(item -> item.quantity())
					.sum());
		runtime.command(owner, delivery);
		assertEquals(gold + 30, queries.find(owner).gold());
		assertEquals(1,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("JOURNEY_HANDS"))
					.mapToInt(item -> item.quantity())
					.sum());
	}

	/** 两个指定精英各自需要新胜利和独立印记，只有一种印记不能领取装备或被提前扣除。 */
	@Test
	void eliteMarksRequireBothVictoriesAndConsumeBothMarksOnce() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		var content = catalog.step("N047");
		String id = "TEST_ELITE_MARKS";
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
		var state = accept(owner, id);
		transactions.run(() -> {
			facts.record(owner, "VICTORY:NOM_QUARRY_ELITE");
			facts.record(owner, "VICTORY:NOM_QUARRY_ELITE");
			inventoryCommands.grant(owner, "TWO_QUARRY_MARKS", "NOM_QUARRY_MARK", 2, false);
		});
		var before = inventoryQueries.list(owner);
		long gold = queries.find(owner).gold();
		assertThrows(RuntimeException.class, () -> runtime.command(owner, request(state, QuestOperation.DELIVER, id)));
		assertEquals(before, inventoryQueries.list(owner));
		assertEquals(gold, queries.find(owner).gold());
		// 只拿到另一枚印记仍不算亲自完成接取后的另一处悬赏。
		transactions.run(() -> inventoryCommands.grant(owner, "CAMP_MARK", "NOM_CAMP_MARK", 1, false));
		assertThrows(RuntimeException.class, () -> runtime.command(owner, request(state, QuestOperation.DELIVER, id)));
		transactions.run(() -> facts.record(owner, "VICTORY:NOM_CAMP_ELITE"));
		var delivery = request(state, QuestOperation.DELIVER, id);
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		assertEquals(1,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("NOM_QUARRY_MARK"))
					.mapToInt(item -> item.quantity())
					.sum());
		assertTrue(inventoryQueries.list(owner).stream().noneMatch(item -> item.code().equals("NOM_CAMP_MARK")));
		assertEquals(1,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("PLAIN_LEGS"))
					.mapToInt(item -> item.quantity())
					.sum());
		assertEquals(gold + 30, queries.find(owner).gold());
		var settled = inventoryQueries.list(owner);
		runtime.command(owner, delivery);
		assertEquals(settled, inventoryQueries.list(owner));
		assertEquals(gold + 30, queries.find(owner).gold());
	}

	/** 正式残件委托须交足六份；不足不扣料，成功扣料和报酬只结算一次。 */
	@Test
	void relicFragmentsRequireFullDeliveryAndSettleOnce() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		var content = catalog.step("N058");
		String id = "TEST_RELIC_DELIVERY";
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
		var state = accept(owner, id);
		transactions.run(() -> inventoryCommands.grant(owner, "RELIC_FIVE", "NOM_RELIC_FRAGMENT", 5, false));
		var before = inventoryQueries.list(owner);
		long gold = queries.find(owner).gold();
		assertThrows(RuntimeException.class, () -> runtime.command(owner, request(state, QuestOperation.DELIVER, id)));
		assertEquals(before, inventoryQueries.list(owner));
		assertEquals(gold, queries.find(owner).gold());
		transactions.run(() -> inventoryCommands.grant(owner, "RELIC_LAST", "NOM_RELIC_FRAGMENT", 1, false));
		var delivery = request(state, QuestOperation.DELIVER, id);
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		assertTrue(inventoryQueries.list(owner).stream().noneMatch(item -> item.code().equals("NOM_RELIC_FRAGMENT")));
		assertEquals(gold + 30, queries.find(owner).gold());
		var settled = inventoryQueries.list(owner);
		runtime.command(owner, delivery);
		assertEquals(settled, inventoryQueries.list(owner));
		assertEquals(gold + 30, queries.find(owner).gold());
	}

	/** 两种材料必须分别足量；失败不消费，成功与报酬共同结算且不可重放扣料。 */
	@Test
	void twoMaterialDeliveryRequiresBothKindsAndConsumesOnlyOnce() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		// 使用正式N052的目标与效果，测试场景仅移到出生点并简化前置以隔离事务验证。
		var content = catalog.step("N052");
		String id = "TEST_TWO_MATERIALS";
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
		transactions.run(() -> inventoryCommands.grant(owner, "DELIVERY_WOOD", "LIFE_WOOD_0", 8, false));
		transactions.run(() -> inventoryCommands.grant(owner, "DELIVERY_ORE", "LIFE_ORE_0", 3, false));
		var state = accept(owner, id);
		var before = inventoryQueries.list(owner);
		long gold = queries.find(owner).gold();
		assertThrows(RuntimeException.class, () -> runtime.command(owner, request(state, QuestOperation.DELIVER, id)));
		assertEquals(before, inventoryQueries.list(owner));
		assertEquals(gold, queries.find(owner).gold());
		transactions.run(() -> inventoryCommands.grant(owner, "DELIVERY_ORE_LAST", "LIFE_ORE_0", 1, false));
		var delivery = request(state, QuestOperation.DELIVER, id);
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		assertEquals(4,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("LIFE_WOOD_0"))
					.mapToInt(item -> item.quantity())
					.sum());
		assertEquals(0,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("LIFE_ORE_0"))
					.mapToInt(item -> item.quantity())
					.sum());
		assertEquals(gold + 30, queries.find(owner).gold());
		var settled = inventoryQueries.list(owner);
		runtime.command(owner, delivery);
		assertEquals(settled, inventoryQueries.list(owner));
		assertEquals(gold + 30, queries.find(owner).gold());
	}

	/** 正式装备教学要求新制造及当前持有同时成立，旧成品、历史事件都不能替代。 */
	@Test
	void equipmentCraftingRequiresNewProductionAndRetainedProduct() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		var content = catalog.step("N034");
		String id = "TEST_CRAFT_EQUIPMENT";
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
		transactions.run(() -> {
			inventoryCommands.grant(owner, "OLD_SWORD", "CRAFT_1_SWORD", 1, true);
			facts.record(owner, "CRAFT_PRODUCT:CRAFT_1_SWORD");
		});
		var state = accept(owner, id);
		assertThrows(RuntimeException.class, () -> runtime.command(owner, request(state, QuestOperation.DELIVER, id)));
		// 制造服务真实发出事件已由LifeCraftingFlowTest覆盖；这里验证正式任务对事件种类和接取窗口的判断。
		transactions.run(() -> facts.record(owner, "CRAFT_PRODUCT:LIFE_PART_0"));
		assertThrows(RuntimeException.class, () -> runtime.command(owner, request(state, QuestOperation.DELIVER, id)));
		transactions.run(() -> facts.record(owner, "CRAFT_PRODUCT:CRAFT_1_SWORD"));
		var before = inventoryQueries.list(owner);
		var delivery = request(state, QuestOperation.DELIVER, id);
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		assertEquals(before, inventoryQueries.list(owner));
		runtime.command(owner, delivery);
		assertEquals(before, inventoryQueries.list(owner));
		long withoutProduct = create();
		var missing = accept(withoutProduct, id);
		transactions.run(() -> facts.record(withoutProduct, "CRAFT_PRODUCT:CRAFT_1_SWORD"));
		assertThrows(RuntimeException.class,
				() -> runtime.command(withoutProduct, request(missing, QuestOperation.DELIVER, id)));
	}

	/** 装备展示使用当前归属，重复交付不消费。 */
	@Test
	void equipmentHoldingRequiresOwnedEquipmentAndDoesNotConsumeIt() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		String id = "TEST_PRESENT_EQUIPMENT";
		configure(id, QuestRepeatPolicy.ONCE, 1, 1, null);
		var base = catalog.step(id);
		var step = new QuestContentStep(id, id, "检查人物装备", base.location(), "检查装备", "检查完成", List.of(), true,
				base.effects(), List.of(new QuestObjective("owned", QuestMeasure.HOLDING, "ITEM:STARTER_WEAPON", 1)),
				List.of(), QuestDelivery.MANUAL, null, null, null, true, List.of(),
				new QuestReception(base.location(), null));
		var all = new ArrayList<>(catalog.definition().steps());
		all.removeIf(value -> value.id().equals(id));
		all.add(step);
		doReturn(new QuestContentDefinition(all)).when(catalog).definition();
		doReturn(step).when(catalog).step(id);
		long owner = create();
		long other = create();
		transactions.run(() -> inventoryCommands.grant(other, "OTHER_EQUIPMENT", "STARTER_WEAPON", 1, true));
		transactions.run(() -> inventoryCommands.grant(owner, "WRONG_ITEM", "HEALTH_POTION", 1, false));
		var state = accept(owner, id);
		assertEquals(0, runtime.views(owner).getLast().goals().getFirst().value());
		assertThrows(RuntimeException.class, () -> runtime.command(owner, request(state, QuestOperation.DELIVER, id)));
		transactions.run(() -> inventoryCommands.grant(owner, "OWN_EQUIPMENT", "STARTER_WEAPON", 1, true));
		assertEquals(1, runtime.views(owner).getLast().goals().getFirst().value());
		var before = inventoryQueries.list(owner);
		var delivery = request(state, QuestOperation.DELIVER, id);
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, delivery).instance().status());
		assertEquals(before, inventoryQueries.list(owner));
		runtime.command(owner, delivery);
		assertEquals(before, inventoryQueries.list(owner));
	}

	/** 展示支持任务前获得、仓库存放、重复交付；其他角色的同种幻兽不能代替当前拥有。 */
	@Test
	void ownedCreaturePresentationIncludesStorageWithoutConsumingOrRequiringCapture() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		String id = "TEST_PRESENT_PET";
		configure(id, QuestRepeatPolicy.ONCE, 1, 1, null);
		var base = catalog.step(id);
		var step = new QuestContentStep(id, id, "展示已有伙伴", base.location(), "展示伙伴（不消耗）", "展示完成", List.of(), true,
				base.effects(), List.of(new QuestObjective("owned", QuestMeasure.HOLDING, "PET:CLAW_CAT", 1)),
				List.of(), QuestDelivery.MANUAL, null, null, null, true, List.of(),
				new QuestReception(base.location(), null));
		var all = new ArrayList<>(catalog.definition().steps());
		all.removeIf(value -> value.id().equals(id));
		all.add(step);
		doReturn(new QuestContentDefinition(all)).when(catalog).definition();
		doReturn(step).when(catalog).step(id);
		long owner = create();
		long other = create();
		// 另一个角色拥有同种幻兽不满足条件；当前角色后续获得的赠予也无需捕捉事件。
		transactions.run(() -> creatures.grant(other, "PRESENT_OTHER", "CLAW_CAT"));
		// 接取前已有的赠予个体同样有效，证明展示不是“接取后重新捕获”的另一种写法。
		var existing = accept(other, id);
		assertEquals(QuestStatus.COMPLETED,
				runtime.command(other, request(existing, QuestOperation.DELIVER, id)).instance().status());
		var state = accept(owner, id);
		assertEquals(0, runtime.views(owner).getLast().goals().getFirst().value());
		assertThrows(RuntimeException.class, () -> runtime.command(owner, request(state, QuestOperation.DELIVER, id)));
		transactions.run(() -> creatures.grant(owner, "PRESENT_GIFT", "CLAW_CAT"));
		var pet = creatureQueries.list(owner).getFirst();
		// 赠予的默认利爪猫为3级；先正常提升人物，避免仓储准备被“伙伴等级高于人物”拒绝。
		transactions.run(() -> characters.grantExperience(owner, 10000));
		assertTrue(queries.find(owner).level() >= pet.level());
		// 沿正式相邻道路到中心，先调至携带位再存仓，使用真实仓储规则而非伪造-1位置。
		for (String place : List.of("STONE_PATH", "SQUARE", "PET_CENTER"))
			characters.move(owner, place, queries.find(owner).revision());
		creatures.position(owner, Long.parseLong(pet.id()), 2);
		creatureManagement.deposit(owner, Long.parseLong(pet.id()));
		for (String place : List.of("SQUARE", "STONE_PATH", "VILLAGE_HOME"))
			characters.move(owner, place, queries.find(owner).revision());
		var before = creatureQueries.list(owner);
		assertEquals(1, runtime.views(owner).getLast().goals().getFirst().value());
		var request = request(state, QuestOperation.DELIVER, id);
		var settled = runtime.command(owner, request);
		assertEquals(QuestStatus.COMPLETED, settled.instance().status());
		assertEquals(before, creatureQueries.list(owner));
		assertEquals(settled, runtime.command(owner, request));
		assertEquals(before, creatureQueries.list(owner));
	}

	/** 指定物种捕捉只累计新捕获，历史捕获、赠予、其他物种及来源重试都不能冒充进度。 */
	@Test
	void speciesCaptureUsesNewCaptureEventsAndIgnoresSourceReplay() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		String id = "TEST_CAPTURE_SPECIES";
		configure(id, QuestRepeatPolicy.ONCE, 1, 2, null);
		var base = catalog.step(id);
		var step = new QuestContentStep(id, id, "捕捉利爪猫", base.location(), "确认成果", "已完成", List.of(), true,
				base.effects(), List.of(new QuestObjective("capture", QuestMeasure.EVENT, "CAPTURE:CLAW_CAT", 2)),
				List.of(), QuestDelivery.MANUAL, null, null, null, true, List.of(),
				new QuestReception(base.location(), null));
		var all = new ArrayList<>(catalog.definition().steps());
		all.removeIf(value -> value.id().equals(id));
		all.add(step);
		doReturn(new QuestContentDefinition(all)).when(catalog).definition();
		doReturn(step).when(catalog).step(id);
		long owner = create();
		transactions.run(() -> creatures.capture(owner, "SPECIES_OLD", "CLAW_CAT", 1, "A", "完好防护", List.of("水弹")));
		var state = accept(owner, id);
		transactions.run(() -> creatures.grant(owner, "SPECIES_GIFT", "CLAW_CAT"));
		transactions.run(() -> creatures.capture(owner, "SPECIES_OLD", "CLAW_CAT", 1, "A", "完好防护", List.of("水弹")));
		transactions.run(() -> creatures.capture(owner, "SPECIES_OTHER", "SPROUT", 1, "A", "完好防护", List.of("藤鞭")));
		assertEquals(0, runtime.views(owner).getLast().goals().getFirst().value());
		transactions.run(() -> creatures.capture(owner, "SPECIES_NEW", "CLAW_CAT", 1, "A", "完好防护", List.of("水弹")));
		transactions.run(() -> creatures.capture(owner, "SPECIES_NEW", "CLAW_CAT", 1, "A", "完好防护", List.of("水弹")));
		assertEquals(1, runtime.views(owner).getLast().goals().getFirst().value());
		assertThrows(RuntimeException.class, () -> runtime.command(owner, request(state, QuestOperation.DELIVER, id)));
		transactions.run(() -> creatures.capture(owner, "SPECIES_NEW_TWO", "CLAW_CAT", 1, "A", "完好防护", List.of("水弹")));
		assertEquals(QuestStatus.COMPLETED,
				runtime.command(owner, request(state, QuestOperation.DELIVER, id)).instance().status());
	}

	/** 每日任务跨周期开启新实例，旧READY资格保留；重复交付请求完整重放且不会再次发金币。 */
	@Test
	void dailyInstancesAndRequestReplaySurviveTransactionBoundaries() {
		// 真实接取事实由系统时钟生成；模拟任务时钟必须始终在其之后。
		// 取服务器时区明天的23:59，避免运行日期超过固定日期后被判定为未来事件。
		var zone = ZoneId.of("Asia/Hong_Kong");
		Instant today = Instant.now().atZone(zone).toLocalDate().plusDays(1).atTime(23, 59).atZone(zone).toInstant();
		doReturn(today).when(clock).now();
		configure("TEST_DAILY", QuestRepeatPolicy.DAILY, 1, 2, null);
		long owner = create();
		var first = accept(owner, "TEST_DAILY");
		record(owner);
		record(owner);
		var ready = runtime.state(owner).getLast();
		assertEquals(QuestStatus.READY, ready.status());
		doReturn(today.plusSeconds(60)).when(clock).now();
		var request = request(ready, QuestOperation.DELIVER, "TEST_DAILY");
		long before = queries.find(owner).gold();
		var settled = runtime.command(owner, request);
		assertEquals(QuestStatus.COMPLETED, settled.instance().status());
		assertEquals(before + 7, queries.find(owner).gold());
		assertEquals(settled, runtime.command(owner, request));
		assertEquals(before + 7, queries.find(owner).gold());
		var second = accept(owner, "TEST_DAILY");
		assertNotEquals(first.id(), second.id());
		assertTrue(second.progress().isEmpty());
		assertThrows(RuntimeException.class, () -> runtime.command(create(), request));
		assertThrows(RuntimeException.class, () -> runtime.command(owner, new QuestInstanceRequest(request.requestId(),
				second.id(), second.round(), request.operation(), request.step(), null, null)));
	}

	/** 真正的回执约束失败会回滚金币和实例完成状态，相同请求号修复后可以安全重试。 */
	@Test
	void receiptFailureRollsBackAssetsAndInstanceTogether() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		configure("TEST_ATOMIC", QuestRepeatPolicy.ONCE, 1, 1, null);
		long owner = create();
		var state = accept(owner, "TEST_ATOMIC");
		record(owner);
		var request = request(state, QuestOperation.DELIVER, "TEST_ATOMIC");
		long before = queries.find(owner).gold();
		jdbc.execute("alter table quest_runtime_request add constraint test_runtime_failure check (false) not valid");
		try {
			assertThrows(RuntimeException.class, () -> runtime.command(owner, request));
		}
		finally {
			jdbc.execute("alter table quest_runtime_request drop constraint test_runtime_failure");
		}
		assertEquals(before, queries.find(owner).gold());
		assertEquals(QuestStatus.READY, runtime.state(owner).getLast().status());
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, request).instance().status());
		assertEquals(before + 7, queries.find(owner).gold());
	}

	/** 环内第二轮拒绝第一轮旧命令；放弃不返还开启次数，次日可创建从第一轮开始的新实例。 */
	@Test
	void ringRoundAndDailyOpeningQuotaAreEnforcedByHost() {
		var today = Instant.now().plusSeconds(60);
		doReturn(today).when(clock).now();
		configure("TEST_RING", QuestRepeatPolicy.RING, 1, 1, null);
		long owner = create();
		var first = accept(owner, "TEST_RING");
		record(owner);
		var request = request(first, QuestOperation.DELIVER, "TEST_RING");
		var next = runtime.command(owner, request).instance();
		assertEquals(2, next.round());
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, new QuestInstanceRequest(UUID.randomUUID().toString(), first.id(), 1,
						QuestOperation.DELIVER, "TEST_RING", null, null)));
		runtime.command(owner, request(next, QuestOperation.ABANDON, null));
		assertThrows(RuntimeException.class, () -> accept(owner, "TEST_RING"));
		doReturn(today.plusSeconds(86400)).when(clock).now();
		var restarted = accept(owner, "TEST_RING");
		assertEquals(1, restarted.round());
		assertNotEquals(first.id(), restarted.id());
	}

	/** 任务超时后重试清空计数，并且使用新的实例与完整计时窗口。 */
	@Test
	void timeoutCreatesFreshAttempt() {
		var start = Instant.now().plusSeconds(60);
		doReturn(start).when(clock).now();
		configure("TEST_TIMED", QuestRepeatPolicy.ONCE, 1, 2, new QuestTimeout(30L, null));
		long owner = create();
		var first = accept(owner, "TEST_TIMED");
		record(owner);
		doReturn(start.plusSeconds(30)).when(clock).now();
		var failed = runtime.command(owner, request(first, QuestOperation.REFRESH, null)).instance();
		assertEquals(QuestStatus.FAILED, failed.status());
		var retry = accept(owner, "TEST_TIMED");
		assertNotEquals(first.id(), retry.id());
		assertTrue(retry.progress().isEmpty());
	}

	/** 已交付的旧步骤保留历史奖励，但不能阻止限时新尝试从入口重新开始。 */
	@Test
	void timedRetryCanRestartPreviouslyDeliveredEntry() {
		var start = Instant.now().plusSeconds(60);
		doReturn(start).when(clock).now();
		String id = "TEST_TIMED_STAGES";
		configure(id, QuestRepeatPolicy.ONCE, 1, 1, new QuestTimeout(30L, null));
		var entry = catalog.step(id);
		String nextId = id + "_NEXT";
		var next = new QuestContentStep(nextId, id, entry.title(), entry.location(), entry.label(), entry.dialogue(),
				List.of(id), true, entry.effects(), entry.objectives(), entry.conditions(), entry.delivery(),
				entry.branch(), entry.timeout(), null, true, List.of(), new QuestReception(entry.location(), null));
		var all = new ArrayList<>(catalog.definition().steps());
		all.add(next);
		doReturn(new QuestContentDefinition(all)).when(catalog).definition();
		doReturn(next).when(catalog).step(nextId);
		doReturn(dialogues.step("O001")).when(dialogues).step(nextId);
		long owner = create();
		var first = accept(owner, id);
		record(owner);
		var advanced = runtime.command(owner, request(first, QuestOperation.DELIVER, id)).instance();
		assertTrue(advanced.completed().contains(id));
		doReturn(start.plusSeconds(30)).when(clock).now();
		var retry = accept(owner, id);
		assertNotEquals(first.id(), retry.id());
		assertTrue(retry.completed().isEmpty());
		record(owner);
		var secondAdvance = runtime.command(owner, request(retry, QuestOperation.DELIVER, id)).instance();
		assertTrue(secondAdvance.completed().contains(id));
		accept(owner, nextId);
	}

	/** 分支选择、公共汇合、日志完成数与剧情回执使用同一实例规则，未选路线不需要交付。 */
	@Test
	void branchSelectionCompletesOnlyChosenRouteAndCommonJoin() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		String id = "TEST_BRANCH";
		configure(id, QuestRepeatPolicy.ONCE, 1, 1, null);
		var base = catalog.step(id);
		var a = branchStep(base, id, List.of(), new QuestBranch("route", "A"), QuestDelivery.MANUAL);
		var b = branchStep(base, id + "_B", List.of(), new QuestBranch("route", "B"), QuestDelivery.MANUAL);
		var join = branchStep(base, id + "_JOIN", List.of(a.id(), b.id()), null, QuestDelivery.AUTO);
		var all = new ArrayList<>(catalog.definition().steps());
		all.removeIf(step -> step.quest().equals(id));
		all.addAll(List.of(a, b, join));
		doReturn(new QuestContentDefinition(all)).when(catalog).definition();
		for (var step : List.of(a, b, join))
			doReturn(step).when(catalog).step(step.id());
		long owner = create();
		var state = accept(owner, id);
		assertEquals(2, runtime.views(owner).getLast().branches().size());
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, a.id())));
		var chosen = runtime
			.command(owner,
					new QuestInstanceRequest(UUID.randomUUID().toString(), state.id(), 1, QuestOperation.CHOOSE, null,
							"route", "B"))
			.instance();
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, new QuestInstanceRequest(UUID.randomUUID().toString(), state.id(), 1,
						QuestOperation.CHOOSE, null, "route", "A")));
		var complete = runtime.command(owner, request(chosen, QuestOperation.DELIVER, b.id())).instance();
		assertEquals(QuestStatus.COMPLETED, complete.status());
		assertFalse(complete.completed().contains(a.id()));
		assertTrue(questQueries.done(owner, join.id()));
		var entry = questQueries.state(owner)
			.journal()
			.stream()
			.filter(value -> value.id().equals(id))
			.findFirst()
			.orElseThrow();
		assertTrue(entry.completed());
		assertEquals(2, entry.totalSteps());
		assertEquals(2, entry.completedSteps());
	}

	/** SWITCHABLE方法在完成前可改选，切换后旧路关闭，完成新路后服务器移除改选入口。 */
	@Test
	void switchableMethodCanChangeUntilOneRouteCompletes() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		String id = "TEST_SWITCHABLE";
		configure(id, QuestRepeatPolicy.ONCE, 1, 1, null);
		var base = catalog.step(id);
		var first = switchableStep(base, id, "A");
		var second = switchableStep(base, id + "_B", "B");
		var join = branchStep(base, id + "_JOIN", List.of(first.id(), second.id()), null, QuestDelivery.MANUAL);
		var all = new ArrayList<>(catalog.definition().steps());
		all.removeIf(step -> step.quest().equals(id));
		all.addAll(List.of(first, second, join));
		doReturn(new QuestContentDefinition(all)).when(catalog).definition();
		for (var step : List.of(first, second, join))
			doReturn(step).when(catalog).step(step.id());
		long owner = create();
		transactions.run(() -> {
			inventoryCommands.grant(owner, "OR_POTIONS", "HEALTH_POTION", 3, false);
			inventoryCommands.grant(owner, "OR_HERBS", "LIFE_HERB_0", 6, false);
		});
		var state = accept(owner, id);
		var selected = runtime
			.command(owner,
					new QuestInstanceRequest(UUID.randomUUID().toString(), state.id(), 1, QuestOperation.CHOOSE, null,
							"method", "A"))
			.instance();
		assertEquals(2, runtime.views(owner).getLast().branches().size());
		selected = runtime
			.command(owner,
					new QuestInstanceRequest(UUID.randomUUID().toString(), selected.id(), 1, QuestOperation.CHOOSE,
							null, "method", "B"))
			.instance();
		assertEquals("B", selected.choices().get("method"));
		assertEquals(2, runtime.views(owner).getLast().branches().size());
		var beforeDelivery = selected;
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(beforeDelivery, QuestOperation.DELIVER, first.id())));
		selected = runtime.command(owner, request(selected, QuestOperation.DELIVER, second.id())).instance();
		assertEquals(3,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("HEALTH_POTION"))
					.mapToInt(item -> item.quantity())
					.sum());
		assertEquals(0,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("LIFE_HERB_0"))
					.mapToInt(item -> item.quantity())
					.sum());
		assertEquals(QuestStatus.READY, selected.status());
		assertTrue(runtime.views(owner).getLast().branches().isEmpty());
		var completedMethod = selected;
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, new QuestInstanceRequest(UUID.randomUUID().toString(),
						completedMethod.id(), completedMethod.round(), QuestOperation.CHOOSE, null, "method", "A")));
		assertEquals(QuestStatus.COMPLETED,
				runtime.command(owner, request(selected, QuestOperation.DELIVER, join.id())).instance().status());
	}

	/** 使用正式工坊两方案验证各自消耗、互斥选择及共同汇合，不能只靠任务数量证明分支落地。 */
	@Test
	void workshopChoicesConsumeOnlySelectedMaterialsAndReachCommonJoin() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		var repair = catalog.step("N053_REPAIR");
		var build = catalog.step("N053_BUILD");
		for (String option : List.of("REPAIR", "BUILD")) {
			String id = "TEST_WORKSHOP_" + option;
			configure(id, QuestRepeatPolicy.ONCE, 1, 1, null);
			var base = catalog.step(id);
			var routes = new ArrayList<QuestContentStep>();
			for (var source : List.of(repair, build)) {
				String routeId = source == repair ? id : id + "_BUILD";
				routes.add(new QuestContentStep(routeId, id, source.title(), base.location(), source.label(),
						source.dialogue(), List.of(), false, source.effects(), source.objectives(), List.of(),
						QuestDelivery.MANUAL, source.branch(), null, null, true, List.of(),
						new QuestReception(base.location(), null)));
			}
			var join = branchStep(base, id + "_JOIN", routes.stream().map(QuestContentStep::id).toList(), null,
					QuestDelivery.AUTO);
			var all = new ArrayList<>(catalog.definition().steps());
			all.removeIf(step -> step.quest().equals(id));
			all.addAll(routes);
			all.add(join);
			doReturn(new QuestContentDefinition(all)).when(catalog).definition();
			for (var step : routes)
				doReturn(step).when(catalog).step(step.id());
			doReturn(join).when(catalog).step(join.id());
			long owner = create();
			transactions.run(() -> {
				inventoryCommands.grant(owner, "WORKSHOP_WOOD", "LIFE_WOOD_0", 4, false);
				inventoryCommands.grant(owner, "WORKSHOP_ORE", "LIFE_ORE_0", 4, false);
			});
			var state = accept(owner, id);
			assertEquals(2, runtime.views(owner).getLast().branches().size());
			var chosen = runtime
				.command(owner,
						new QuestInstanceRequest(UUID.randomUUID().toString(), state.id(), 1, QuestOperation.CHOOSE,
								null, "NOM_WORKSHOP", option))
				.instance();
			int index = option.equals("REPAIR") ? 0 : 1;
			assertThrows(RuntimeException.class,
					() -> runtime.command(owner, request(chosen, QuestOperation.DELIVER, routes.get(1 - index).id())));
			var delivered = runtime.command(owner, request(chosen, QuestOperation.DELIVER, routes.get(index).id()))
				.instance();
			assertEquals(QuestStatus.COMPLETED, delivered.status());
			assertTrue(questQueries.done(owner, join.id()));
			String consumed = option.equals("REPAIR") ? "LIFE_WOOD_0" : "LIFE_ORE_0";
			String retained = option.equals("REPAIR") ? "LIFE_ORE_0" : "LIFE_WOOD_0";
			assertEquals(0,
					inventoryQueries.list(owner)
						.stream()
						.filter(item -> item.code().equals(consumed))
						.mapToInt(item -> item.quantity())
						.sum());
			assertEquals(4,
					inventoryQueries.list(owner)
						.stream()
						.filter(item -> item.code().equals(retained))
						.mapToInt(item -> item.quantity())
						.sum());
		}
	}

	/** 同一微秒完成多次每日任务仍以尝试序号定位最新实例，并准确限制次数。 */
	@Test
	void sameTimestampAttemptsRemainOrderedAndRespectQuota() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		configure("TEST_MULTI_DAILY", QuestRepeatPolicy.DAILY, 2, 1, null);
		long owner = create();
		var first = accept(owner, "TEST_MULTI_DAILY");
		record(owner);
		runtime.command(owner, request(first, QuestOperation.DELIVER, "TEST_MULTI_DAILY"));
		var second = accept(owner, "TEST_MULTI_DAILY");
		assertNotEquals(first.id(), second.id());
		assertEquals(second.id(), runtime.latest(queries.requireId(owner), "TEST_MULTI_DAILY").id());
		assertTrue(second.progress().isEmpty());
		record(owner);
		runtime.command(owner, request(second, QuestOperation.DELIVER, "TEST_MULTI_DAILY"));
		assertThrows(RuntimeException.class, () -> accept(owner, "TEST_MULTI_DAILY"));
		assertTrue(questQueries.state(owner)
			.available()
			.stream()
			.noneMatch(value -> value.quest().equals("TEST_MULTI_DAILY")));
	}

	/** 同时刻新尝试不能复用旧补给资格，环内每轮补给独立且同轮重接保持幂等。 */
	@Test
	void suppliesAreScopedToAttemptAndRingRound() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		String id = "TEST_RING_SUPPLIES";
		configure(id, QuestRepeatPolicy.RING, 1, 1, null);
		var base = catalog.step(id);
		var step = new QuestContentStep(id, id, base.title(), "VILLAGE_HOME", base.label(), base.dialogue(), List.of(),
				true, base.effects(), base.objectives(), base.conditions(), base.delivery(), base.branch(),
				base.timeout(), null, true, List.of(new QuestEffect("GIVE_ITEM", "HEALTH_POTION", 2)),
				new QuestReception("VILLAGE_HOME", null));
		var all = new ArrayList<>(catalog.definition().steps());
		all.removeIf(value -> value.id().equals(id));
		all.add(step);
		doReturn(new QuestContentDefinition(all)).when(catalog).definition();
		doReturn(step).when(catalog).step(id);
		long owner = create();
		var first = accept(owner, id);
		record(owner);
		var second = runtime.command(owner, request(first, QuestOperation.DELIVER, id)).instance();
		assertEquals(2, second.round());
		record(owner);
		// 下一轮即使已经完成击杀，也须接取本轮补给后才能交付。
		assertThrows(RuntimeException.class, () -> runtime.command(owner, request(second, QuestOperation.DELIVER, id)));
		var grant = acceptance.accept(owner, id,
				new QuestAcceptRequest(UUID.randomUUID().toString(), queries.find(owner).revision()));
		assertEquals(1, grant.rewards().size());
		var repeated = acceptance.accept(owner, id,
				new QuestAcceptRequest(UUID.randomUUID().toString(), queries.find(owner).revision()));
		assertTrue(repeated.rewards().isEmpty());
		assertEquals(3, runtime.command(owner, request(second, QuestOperation.DELIVER, id)).instance().round());
	}

}
