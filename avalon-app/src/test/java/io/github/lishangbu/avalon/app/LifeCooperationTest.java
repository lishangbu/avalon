package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.application.command.BattleCommandService;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.command.CharacterFactCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.battle.GrowthRules;
import io.github.lishangbu.avalon.content.life.GatheringCatalog;
import io.github.lishangbu.avalon.content.life.LifeProductionCatalog;
import io.github.lishangbu.avalon.content.life.LifeSkillRules;
import io.github.lishangbu.avalon.content.life.LifeSkillRulesSkill;
import io.github.lishangbu.avalon.content.life.model.GatheringNode;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.inventory.application.model.InventoryItemView;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.market.application.command.MarketAccess;
import io.github.lishangbu.avalon.market.application.command.MarketListingCommandService;
import io.github.lishangbu.avalon.market.application.command.MarketPurchaseCommandService;
import io.github.lishangbu.avalon.market.application.model.MarketListingBatchRequest;
import io.github.lishangbu.avalon.market.application.model.MarketListingSelection;
import io.github.lishangbu.avalon.market.application.model.MarketPurchaseRequest;
import io.github.lishangbu.avalon.world.application.command.GatheringCommandService;
import io.github.lishangbu.avalon.world.application.command.LifeCraftingService;
import io.github.lishangbu.avalon.world.application.command.LifeSkillCommandService;
import io.github.lishangbu.avalon.world.application.model.GatheringBeat;
import io.github.lishangbu.avalon.world.application.model.GatheringStart;
import io.github.lishangbu.avalon.world.application.model.GatheringStop;
import io.github.lishangbu.avalon.world.application.model.LifeCraftingRequest;
import io.github.lishangbu.avalon.world.application.model.LifeSkillRequest;
import io.github.lishangbu.avalon.world.domain.GatheringRareRoll;
import io.github.lishangbu.avalon.world.domain.LifeClock;
import io.github.lishangbu.avalon.world.domain.LifeToolRandom;
import io.github.lishangbu.avalon.world.persistence.repository.LifeSkillRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import tools.jackson.databind.ObjectMapper;

/** 三个独立专业以真实制造和市场事务完成铁工具合作；成长、原料和初始工具使用存档夹具。 */
class LifeCooperationTest extends SpringIntegrationTest {

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterFactCommandService facts;

	@Autowired
	QuestCatalog quests;

	@Autowired
	WorldCatalog world;

	@Autowired
	GatheringCatalog nodes;

	@Autowired
	GatheringCommandService gathering;

	@Autowired
	CreatureCommandService creatures;

	@Autowired
	BattleCommandService battles;

	@MockitoBean
	LifeClock clock;

	@MockitoBean
	GatheringRareRoll rare;

	private long tick;

	private int recoveries;

	private boolean fullProtocol;

	private final List<String> travel = new ArrayList<>();

	private final List<Map<String, Object>> operations = new ArrayList<>();

	@Autowired
	CharacterQueryService characterQueries;

	@Autowired
	GrowthRules growth;

	@Autowired
	InventoryCommandService inventory;

	@Autowired
	InventoryQueryService items;

	@Autowired
	LifeSkillCommandService learning;

	@Autowired
	LifeSkillRepository skills;

	@Autowired
	LifeCraftingService crafting;

	@Autowired
	LifeProductionCatalog recipes;

	@Autowired
	MarketAccess access;

	@Autowired
	MarketListingCommandService listings;

	@Autowired
	MarketPurchaseCommandService purchases;

	@Autowired
	JdbcClient jdbc;

	@MockitoBean
	LifeToolRandom random;

	@ParameterizedTest
	@ValueSource(ints = { 11, 15 })
	void copperSwordCanBeMadeFromGatheredAndTradedSingleReceiptMaterials(int petLevel) throws Exception {
		when(random.breaks(anyInt())).thenReturn(false);
		when(clock.now()).thenAnswer(call -> Instant.ofEpochMilli(tick));
		long maker = ready(LifeSkillRulesSkill.ARTISAN, Map.of());
		transactions.run(() -> characters.changeGold(maker, 3000));
		trade(collect("LIFE_ORE_1", 192, petLevel, petLevel == 15), maker, "LIFE_ORE_1", 192);
		trade(collect("LIFE_WOOD_1", 96, petLevel, petLevel == 15), maker, "LIFE_WOOD_1", 96);
		make(maker, "CRAFT_3_SWORD", "TOOL_COPPER_HAMMER");
		assertEquals(1, item(maker, "CRAFT_3_SWORD").quantity());
		assertFalse(item(maker, "CRAFT_3_SWORD").bound());
		assertTrue(recoveries > 0);
		if (petLevel == 15)
			location(maker, "CITY_SHOP");
		long heartbeatMillis = 0;
		long commands = travel.size() + recoveries;
		for (var operation : operations) {
			if (operation.get("type").equals("GATHER")) {
				heartbeatMillis += ((Number) operation.get("elapsedMillis")).longValue();
				commands += ((Number) operation.get("starts")).longValue()
						+ ((Number) operation.get("stops")).longValue();
			}
			else if (operation.get("type").equals("TRADE"))
				commands += 2;
			else if (operation.get("type").equals("CRAFT"))
				commands++;
		}
		String report = fullProtocol ? "copper-sword-town-operations.json"
				: petLevel == 11 ? "copper-sword-operations.json" : "copper-sword-efficient-operations.json";
		Files.writeString(Path.of("target", report), new ObjectMapper().writerWithDefaultPrettyPrinter()
			.writeValueAsString(Map.of("scope",
					petLevel + "级采集者及伙伴、5级专业、初始工具金币剧情预置；" + (petLevel == 15 ? "选择同档最高产量节点及铜工具。" : "选择最低等级铜节点及铁工具。")
							+ "正式采集、恢复、移动、同种材料合并交易制造；时钟可控、无工具报废；单价10仅为测试，不是真人耗时。",
					"recoveries", recoveries, "travel", travel, "operations", operations, "commands", commands,
					"commandSeconds", commands * 2, "protocolSeconds", heartbeatMillis / 1000.0 + commands * 2)));
	}

	@Test
	void threeProfessionsTradeAuxiliariesAndReturnAnIronToolToItsUser() throws Exception {
		when(random.breaks(anyInt())).thenReturn(false);
		when(clock.now()).thenAnswer(call -> Instant.ofEpochMilli(tick));
		long artisan = ready(LifeSkillRulesSkill.ARTISAN, Map.of());
		long leather = ready(LifeSkillRulesSkill.LEATHERWORK, Map.of());
		long alchemist = ready(LifeSkillRulesSkill.ALCHEMY, Map.of());
		long miner = collect("LIFE_ORE_1", 5);
		trade(miner, artisan, "LIFE_ORE_1", 3);
		trade(miner, leather, "LIFE_ORE_1", 2);
		trade(collect("LIFE_ORE_2", 3), artisan, "LIFE_ORE_2", 3);
		trade(collect("LIFE_HIDE_1", 3), leather, "LIFE_HIDE_1", 3);
		trade(collect("LIFE_WOOD_1", 2), leather, "LIFE_WOOD_1", 2);
		trade(collect("LIFE_HERB_1", 3), alchemist, "LIFE_HERB_1", 3);
		make(artisan, "LIFE_PART_1", "TOOL_COPPER_HAMMER");
		make(alchemist, "LIFE_REAGENT_1", "TOOL_COPPER_ROD");
		trade(artisan, leather, "LIFE_PART_1");
		trade(alchemist, leather, "LIFE_REAGENT_1");
		make(leather, "LIFE_WRAP_1", "TOOL_COPPER_NEEDLE");
		make(leather, "TOOL_IRON_HAMMER", "TOOL_COPPER_NEEDLE");
		assertFalse(item(leather, "TOOL_IRON_HAMMER").bound(), "绑定生产工具不应污染无绑定材料的成品");
		trade(leather, artisan, "TOOL_IRON_HAMMER");
		make(artisan, "LIFE_PART_2", "TOOL_IRON_HAMMER");
		assertEquals(1, item(artisan, "LIFE_PART_2").quantity());
		assertFalse(item(artisan, "LIFE_PART_2").bound());
		assertFalse(items.list(leather).stream().anyMatch(i -> i.code().equals("TOOL_IRON_HAMMER")));
		assertEquals(1, skills.owned(characterQueries.requireId(artisan)).size());
		assertEquals(1, skills.owned(characterQueries.requireId(leather)).size());
		assertEquals(1, skills.owned(characterQueries.requireId(alchemist)).size());
		assertTrue(travel.stream().anyMatch(step -> step.endsWith("->GNOME_FORGE")));
		assertTrue(travel.stream().anyMatch(step -> step.endsWith("->GNOME_LEATHERWORKS")));
		assertTrue(travel.stream().anyMatch(step -> step.endsWith("->GNOME_APOTHECARY")));
		System.out.println("跨专业合作实际移动：" + travel.size() + "步 " + travel);
		assertEquals(5, operations.stream().filter(o -> o.get("type").equals("GATHER")).count());
		assertEquals(5, operations.stream().filter(o -> o.get("type").equals("CRAFT")).count());
		Files.writeString(Path.of("target/life-cooperation-operations.json"),
				new ObjectMapper().writerWithDefaultPrettyPrinter()
					.writeValueAsString(Map.of("scope",
							"正式采集、走图、同种材料合并交易和制造命令；重放不计操作数。初始等级、工具、剧情和金币预置；时钟可控且不报废。"
									+ "采集耗时为服务端规则计时，移动交易制造次数不是点击数或真人耗时；成交单价10为测试价格。",
							"travel", travel, "operations", operations)));
	}

	/** 双方从城镇出发并返回；补造采用真实采集交易制造小链，按正式报废概率递归。 */
	@Test
	void fullTownProtocolIncludesReplacementLogisticsAndWaitingSensitivity() throws Exception {
		fullProtocol = true;
		copperSwordCanBeMadeFromGatheredAndTradedSingleReceiptMaterials(15);
		var base = measured();
		var replacements = new TreeMap<String, Measured>();
		for (String code : List.of("TOOL_COPPER_PICKAXE", "TOOL_COPPER_AXE", "TOOL_COPPER_HAMMER", "TOOL_COPPER_NEEDLE",
				"TOOL_COPPER_ROD")) {
			travel.clear();
			operations.clear();
			recoveries = 0;
			var recipe = recipes.recipe(code);
			long maker = ready(LifeSkillRulesSkill.valueOf(recipe.skill()), Map.of());
			for (var ingredient : recipe.ingredients().entrySet())
				trade(collect(ingredient.getKey(), ingredient.getValue(), 15, true), maker, ingredient.getKey(),
						ingredient.getValue());
			String tool = "TOOL_COPPER_" + LifeSkillRulesSkill.valueOf(recipe.skill()).tool;
			make(maker, code, tool);
			long recipient = ready(LifeSkillRulesSkill.MINING, Map.of());
			trade(maker, recipient, code);
			assertEquals(1, item(recipient, code).quantity());
			replacements.put(code, measured());
		}
		var costs = new TreeMap<String, Double>();
		var trades = new TreeMap<String, Double>();
		replacements.keySet().forEach(code -> {
			costs.put(code, 0.0);
			trades.put(code, 0.0);
		});
		boolean converged = false;
		for (int iteration = 0; iteration < 100; iteration++) {
			var next = new TreeMap<String, Double>();
			var nextTrades = new TreeMap<String, Double>();
			double delta = 0;
			for (var entry : replacements.entrySet()) {
				double seconds = entry.getValue().seconds();
				double exchanges = entry.getValue().trades();
				for (var dependency : entry.getValue().breaks().entrySet()) {
					assertTrue(costs.containsKey(dependency.getKey()), "补造链缺少工具：" + dependency.getKey());
					seconds += dependency.getValue() * costs.get(dependency.getKey());
					exchanges += dependency.getValue() * trades.get(dependency.getKey());
				}
				next.put(entry.getKey(), seconds);
				nextTrades.put(entry.getKey(), exchanges);
				delta = Math.max(delta, Math.max(Math.abs(seconds - costs.get(entry.getKey())),
						Math.abs(exchanges - trades.get(entry.getKey()))));
			}
			costs.clear();
			costs.putAll(next);
			trades.clear();
			trades.putAll(nextTrades);
			if (delta < 1e-8) {
				converged = true;
				break;
			}
		}
		assertTrue(converged);
		double manufacturing = base.seconds();
		double exchanges = base.trades();
		for (var broken : base.breaks().entrySet()) {
			manufacturing += broken.getValue() * costs.get(broken.getKey());
			exchanges += broken.getValue() * trades.get(broken.getKey());
		}
		assertTrue(manufacturing > base.seconds());
		for (var entry : replacements.entrySet()) {
			double evaluated = entry.getValue().seconds();
			for (var dependency : entry.getValue().breaks().entrySet())
				evaluated += dependency.getValue() * costs.get(dependency.getKey());
			assertEquals(evaluated, costs.get(entry.getKey()), 1e-7, "工具补造方程残差");
		}
		var fixture = new RuleTestFixture();
		var benchmark = new DungeonCombatSamples(fixture.rules, fixture.engine, fixture.battles, fixture.creatures,
				fixture.dungeon, fixture.dungeonPolicy);
		int inside = benchmark.personalRunCommands();
		assertEquals(14, inside);
		int totalWeight = fixture.chests.definition().equipment().stream().mapToInt(e -> e.weight()).sum();
		int weight = fixture.chests.definition()
			.equipment()
			.stream()
			.filter(e -> e.code().equals("CRYSTAL_SWORD"))
			.findFirst()
			.orElseThrow()
			.weight();
		double probability = (double) weight / totalWeight;
		double runs = 1 / probability;
		double days = 1 / (1 - Math.pow(1 - probability, fixture.dungeon.definition().dailyStarts()));
		var comparisons = new ArrayList<Map<String, Object>>();
		// 记录真实移动路径，恢复后回到同一城镇；专业/初始工具与剧情属于已具备基础设施。
		travel.clear();
		long walker = ready(LifeSkillRulesSkill.ARTISAN, Map.of());
		location(walker, fixture.dungeon.definition().entrance());
		location(walker, "CITY_PETS");
		location(walker, "CITY_SHOP");
		int externalMoves = travel.size();
		for (String profession : List.of("WARRIOR", "RANGER", "MAGE", "PRIEST")) {
			double rounds = benchmark.meanRounds(profession, 15, 128);
			// 每天首次出发已经满资源；当天继续入本才在入口恢复，最后回城也恢复。
			double dungeonSeconds = (rounds * 5 + (inside + 4) * 2) * runs + (runs - days) * 2
					+ (externalMoves + 1) * 2 * days;
			double groupSeconds = (rounds * 5 * 3 + (32 + 10) * 2) * runs + (runs - days) * 2 * 3
					+ (externalMoves + 1) * 2 * 3 * days;
			for (int marketWait : List.of(0, 60, 300))
				for (int partyWait : List.of(0, 60, 300)) {
					double craftElapsed = manufacturing + exchanges * marketWait;
					double dungeonElapsed = dungeonSeconds + runs * partyWait;
					comparisons
						.add(Map.of("profession", profession, "marketWaitPerExchange", marketWait, "partyWaitPerRun",
								partyWait, "craftSerialSeconds", craftElapsed, "dungeonLeaderActiveSeconds",
								dungeonSeconds, "dungeonLeaderProtocolSeconds", dungeonElapsed, "ratioToLeader",
								craftElapsed / dungeonElapsed, "dungeonAllParticipantsActiveSeconds", groupSeconds,
								"ratioOfActiveLabour", manufacturing / groupSeconds, "withinTarget",
								craftElapsed / dungeonElapsed >= 1.5 && craftElapsed / dungeonElapsed <= 2));
				}
		}
		Files.writeString(Path.of("target/full-investment-protocol.json"), new ObjectMapper()
			.writerWithDefaultPrettyPrinter()
			.writeValueAsString(Map.of("scope",
					"15级伙伴，已具备专业与初始工具；制造按所有供应者串行工时，副本同时报告队长和全队劳动。每命令2秒、每战斗轮5秒；等待为情景参数；日额度单列。工具长期补货采用备用工具库存，补造含真实采集、恢复、转场、交易与制造，不假定现场即时造出替代工具。",
					"baseline", base, "replacementEpisodes", replacements, "recursiveToolSeconds", costs,
					"manufacturingActiveSeconds", manufacturing, "expectedExchanges", exchanges, "expectedStartDays",
					days, "externalRoute", travel, "comparisons", comparisons)));
	}

	@Test
	void brokenGatheringToolRequiresStopAndRestartWithCarriedReserve() {
		fullProtocol = true;
		when(random.breaks(anyInt())).thenReturn(true, false);
		when(clock.now()).thenAnswer(call -> Instant.ofEpochMilli(tick));
		long owner = collect("LIFE_ORE_1", 4, 15, true);
		assertEquals(4,
				items.list(owner)
					.stream()
					.filter(i -> i.code().equals("LIFE_ORE_1"))
					.mapToInt(InventoryItemView::quantity)
					.sum());
		var operation = operations.stream().filter(o -> o.get("type").equals("GATHER")).findFirst().orElseThrow();
		assertEquals(2, operation.get("starts"));
		assertEquals(2, operation.get("stops"));
		assertEquals(1,
				items.list(owner)
					.stream()
					.filter(i -> i.code().equals("TOOL_COPPER_PICKAXE"))
					.mapToInt(InventoryItemView::quantity)
					.sum());
	}

	private Measured measured() {
		long commands = travel.size() + recoveries;
		double seconds = 0;
		int exchanges = 0;
		var breaks = new TreeMap<String, Double>();
		for (var operation : operations) {
			String type = (String) operation.get("type");
			if (type.equals("GATHER")) {
				seconds += ((Number) operation.get("elapsedMillis")).doubleValue() / 1000;
				commands += ((Number) operation.get("starts")).longValue()
						+ ((Number) operation.get("stops")).longValue();
				String tool = (String) operation.get("tool");
				double expectedBreaks = ((Number) operation.get("rounds")).doubleValue()
						/ recipes.tool(tool).breakDenominator();
				breaks.merge(tool, expectedBreaks, Double::sum);
				// 每次采集中报废额外计停止、换备用工具重新派遣和首个心跳；末轮也计，作为保守上界。
				seconds += expectedBreaks * 5;
			}
			else if (type.equals("TRADE")) {
				commands += 2;
				exchanges++;
			}
			else if (type.equals("CRAFT")) {
				commands++;
				String tool = (String) operation.get("tool");
				breaks.merge(tool, 1.0 / recipes.tool(tool).breakDenominator(), Double::sum);
			}
		}
		return new Measured(seconds + commands * 2, commands, recoveries, exchanges, Map.copyOf(breaks),
				List.copyOf(travel), List.copyOf(operations));
	}

	private record Measured(double seconds, long commands, int recoveries, int trades, Map<String, Double> breaks,
			List<String> travel, List<Map<String, Object>> operations) {
	}

	private long ready(LifeSkillRulesSkill skill, Map<String, Integer> raw) {
		String name = "coop_" + UUID.randomUUID().toString().substring(0, 8);
		registrations.register(name, "Life-test-12345", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		transactions.run(() -> characters.grantExperience(owner, Math.toIntExact(growth.threshold(11))));
		learning.change(owner, new LifeSkillRequest(UUID.randomUUID().toString(), skill, false), false);
		transactions.run(() -> {
			characters.lock(owner);
			quests.definition().steps().forEach(step -> {
				facts.record(owner, "QUEST:" + step.id());
				step.facts().forEach(fact -> facts.record(owner, fact));
			});
			skills.progress(characterQueries.requireId(owner), skill, LifeSkillRules.threshold(5) * 2);
			inventory.grantStrict(owner, "COPPER_TOOL", "TOOL_COPPER_" + skill.tool, 1, false, true);
			raw.forEach((code, count) -> inventory.grantStrict(owner, "RAW:" + code, code, count, false, false));
		});
		jdbc.sql("UPDATE player_character SET gold=1000, location='CITY_SHOP' WHERE id=:id")
			.param("id", characterQueries.requireId(owner))
			.update();
		location(owner, "CITY_SHOP");
		access.teach(owner, true);
		return owner;
	}

	private void make(long owner, String recipeId, String toolCode) {
		var recipe = recipes.recipe(recipeId);
		location(owner, recipe.locations().getFirst());
		var materials = new TreeMap<String, Integer>();
		recipe.ingredients().forEach((code, count) -> {
			int remaining = count;
			for (var stack : items.list(owner)) {
				if (!stack.code().equals(code) || remaining == 0)
					continue;
				int take = Math.min(remaining, stack.quantity());
				materials.put(stack.id(), take);
				remaining -= take;
			}
			assertEquals(0, remaining, "缺少材料：" + code);
		});
		var request = new LifeCraftingRequest(UUID.randomUUID().toString(), recipeId, 1, item(owner, toolCode).id(), 1,
				materials);
		var result = crafting.craft(owner, request);
		assertEquals(1, result.completed());
		operations.add(Map.of("type", "CRAFT", "recipe", recipeId, "materialStacks", materials.size(), "quantity", 1,
				"tool", toolCode));
		assertEquals(result, crafting.craft(owner, request));
	}

	private void trade(long seller, long buyer, String code) {
		trade(seller, buyer, code, 1);
	}

	private void trade(long seller, long buyer, String code, int quantity) {
		location(seller, "CITY_SHOP");
		location(buyer, "CITY_SHOP");
		var selections = new ArrayList<MarketListingSelection>();
		int remaining = quantity;
		for (var item : items.list(seller)) {
			if (!item.code().equals(code) || item.bound() || remaining == 0)
				continue;
			int count = Math.min(remaining, item.quantity());
			selections.add(new MarketListingSelection(Long.parseLong(item.id()), count, item));
			remaining -= count;
		}
		assertEquals(0, remaining);
		int beforeQuantity = items.list(buyer)
			.stream()
			.filter(i -> i.code().equals(code))
			.mapToInt(InventoryItemView::quantity)
			.sum();
		var listing = listings.listBatch(seller,
				new MarketListingBatchRequest(UUID.randomUUID().toString(), selections, 10));
		var request = new MarketPurchaseRequest(UUID.randomUUID().toString(), Long.parseLong(listing.listingId()),
				quantity, 10);
		var result = purchases.buy(buyer, request);
		operations.add(Map.of("type", "TRADE", "product", code, "quantity", quantity, "listings", 1, "purchases", 1));
		long sellerGold = characterQueries.find(seller).gold();
		long buyerGold = characterQueries.find(buyer).gold();
		assertEquals(result, purchases.buy(buyer, request));
		assertEquals(sellerGold, characterQueries.find(seller).gold());
		assertEquals(buyerGold, characterQueries.find(buyer).gold());
		assertEquals(beforeQuantity + quantity,
				items.list(buyer)
					.stream()
					.filter(i -> i.code().equals(code))
					.mapToInt(InventoryItemView::quantity)
					.sum());
	}

	private long collect(String product, int quantity) {
		return collect(product, quantity, 11, false);
	}

	private long collect(String product, int quantity, int petLevel, boolean efficient) {
		var node = nodes.nodes()
			.stream()
			.filter(n -> n.product().equals(product))
			.min(efficient ? Comparator.comparingInt(GatheringNode::quantity).reversed()
					: Comparator.comparingInt(GatheringNode::level))
			.orElseThrow();
		var skill = LifeSkillRulesSkill.valueOf(node.skill());
		long owner = ready(skill, Map.of());
		String tool = (efficient ? "TOOL_COPPER_" : "TOOL_IRON_") + skill.tool;
		long pet = transactions.call(() -> {
			characters.grantExperience(owner, Math.toIntExact(growth.threshold(petLevel) - growth.threshold(11)));
			inventory.grantStrict(owner, "GATHER_TOOL", tool, 1, false, false);
			return creatures.capture(owner, "GATHER_PET", "CLAW_CAT", petLevel, "A", "完好防护", List.of("水弹"));
		});
		location(owner, node.location());
		long started = tick;
		var session = gathering.begin(owner, new GatheringStart(UUID.randomUUID().toString(), node.id(),
				Long.toString(pet), item(owner, tool).id(), 1));
		int expectedRounds = (quantity + node.quantity() - 1) / node.quantity();
		long previousRounds = 0;
		int starts = 1;
		for (int beat = 0; previousRounds + session.completed() < expectedRounds && beat < 10000; beat++) {
			tick += 1000;
			session = gathering.heartbeat(owner, new GatheringBeat(session.id(), session.revision(), true));
			if (session.status().equals("TOOLS_EXHAUSTED") && previousRounds + session.completed() < expectedRounds) {
				previousRounds += session.completed();
				gathering.stop(owner, new GatheringStop(UUID.randomUUID().toString(), session.id()));
				session = gathering.begin(owner, new GatheringStart(UUID.randomUUID().toString(), node.id(),
						Long.toString(pet), item(owner, tool).id(), 1));
				starts++;
			}
			if (session.status().equals("RESOURCES_LOW")) {
				previousRounds += session.completed();
				gathering.stop(owner, new GatheringStop(UUID.randomUUID().toString(), session.id()));
				location(owner, "CITY_PETS");
				battles.recover(owner);
				recoveries++;
				location(owner, node.location());
				session = gathering.begin(owner, new GatheringStart(UUID.randomUUID().toString(), node.id(),
						Long.toString(pet), item(owner, tool).id(), 1));
				starts++;
			}
		}
		assertEquals(expectedRounds, previousRounds + session.completed());
		operations.add(Map.of("type", "GATHER", "product", product, "rounds", expectedRounds, "quantity",
				expectedRounds * node.quantity(), "elapsedMillis", tick - started, "ruleMillis",
				expectedRounds * session.roundMillis(), "starts", starts, "stops", starts, "tool", tool));
		gathering.stop(owner, new GatheringStop(UUID.randomUUID().toString(), session.id()));
		assertTrue(items.list(owner)
			.stream()
			.filter(i -> i.code().equals(product))
			.mapToInt(InventoryItemView::quantity)
			.sum() >= quantity);
		if (fullProtocol) {
			location(owner, "CITY_PETS");
			battles.recover(owner);
			recoveries++;
			location(owner, "CITY_SHOP");
		}
		return owner;
	}

	private InventoryItemView item(long owner, String code) {
		return items.list(owner).stream().filter(i -> i.code().equals(code)).findFirst().orElseThrow();
	}

	private void location(long owner, String location) {
		var pending = new ArrayDeque<List<String>>();
		var seen = new HashSet<String>();
		pending.add(List.of(characterQueries.find(owner).location()));
		while (!pending.isEmpty()) {
			var path = pending.remove();
			String place = path.getLast();
			if (!seen.add(place))
				continue;
			if (place.equals(location)) {
				for (String step : path.subList(1, path.size())) {
					var before = characterQueries.find(owner);
					characters.move(owner, step, before.revision());
					assertEquals(step, characterQueries.find(owner).location());
					travel.add(before.location() + "->" + step);
				}
				return;
			}
			for (String exit : world.place(place).exits()) {
				var next = new ArrayList<>(path);
				next.add(exit);
				pending.add(next);
			}
		}
		fail("合作设施没有道路：" + location);
	}

}
