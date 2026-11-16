package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.content.life.LifeSkillRules;
import io.github.lishangbu.avalon.content.life.LifeSkillRulesSkill;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.content.battle.GrowthRules;
import io.github.lishangbu.avalon.character.application.command.CharacterFactCommandService;
import io.github.lishangbu.avalon.world.persistence.repository.LifeSkillRepository;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 金档供料的局部真实事务回归：角色专业、资金与银档原料由夹具准备，制造、工具与市场交付均执行正式命令。 本测试只隔离后期供料衔接问题，不冒充四角色从零练到九级的自然成本证据。
 */
@Import({ StoryOpeningJourney.class, NaturalEndgameCraftingJourney.class })
class NaturalEndgameCraftingFlowTest extends SpringIntegrationTest {

	@Autowired
	StoryOpeningJourney journey;

	@Autowired
	NaturalEndgameCraftingJourney supply;

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	LifeSkillRepository skills;

	@Autowired
	GrowthRules growth;

	@Autowired
	CharacterFactCommandService facts;

	@Autowired
	JdbcClient jdbc;

	@org.springframework.test.context.bean.override.mockito.MockitoBean
	io.github.lishangbu.avalon.world.domain.LifeClock clock;

	@org.springframework.test.context.bean.override.mockito.MockitoBean
	io.github.lishangbu.avalon.world.domain.GatheringYieldRoll gatheringYield;

	@org.springframework.test.context.bean.override.mockito.MockitoBean
	io.github.lishangbu.avalon.world.domain.LifeToolRandom toolRandom;

	@org.springframework.test.context.bean.override.mockito.MockitoSpyBean
	io.github.lishangbu.avalon.battle.application.command.BattleCommandService battles;

	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.ValueSource(ints = { 24, 192 })
	void depletedGatheringPartnerRecoversNormallyAndContinuesSupplying(int required) {
		long leatherworker = actor("HUNTING"), alchemist = actor("ALCHEMY"), artisan = actor("ARTISAN"),
				cook = actor("COOKING");
		// 本项只隔离资源耗尽分支：确定产出、不报废工具，避免其他随机分支遮蔽恢复行为。
		// 完整自然路线仍使用正式概率，本夹具不作为九级采集资格或成长成本证据。
		org.mockito.Mockito
			.when(gatheringYield.succeeds(org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
			.thenReturn(true);
		org.mockito.Mockito.when(toolRandom.breaks(org.mockito.ArgumentMatchers.anyInt())).thenReturn(false);
		var tick = new java.util.concurrent.atomic.AtomicLong();
		org.mockito.Mockito.when(clock.now()).thenAnswer(call -> java.time.Instant.ofEpochMilli(tick.addAndGet(1000)));
		transactions.run(() -> {
			facts.record(leatherworker, "QUEST:O015");
			facts.record(leatherworker, "QUEST:N025");
			facts.record(leatherworker, "QUEST:F001");
			facts.record(leatherworker, "QUEST:J025");
			// 一级伙伴可合法派遣，但无法靠一次满资源完成本批金档采集，必须正式回城恢复。
			journey.creatures.grant(leatherworker, "LOW_RESOURCE_PARTNER", "SPROUT");
			journey.inventory.grantStrict(leatherworker, "GOLD_TRAP", "TOOL_GOLD_TRAP", 1, false, false);
		});
		long recipient = required == 24 ? artisan : leatherworker;
		supply.supply(leatherworker, alchemist, artisan, cook, recipient, "LIFE_HIDE_4", required);
		assertEquals(required, quantity(recipient, "LIFE_HIDE_4"));
		org.mockito.Mockito.verify(battles, org.mockito.Mockito.atLeast(2)).recover(leatherworker);
		assertEquals("CITY_SHOP", journey.queries.find(leatherworker).location());
	}

	@Test
	void existingSupplierStockIsDeliveredWithoutManufacturingAgain() {
		long leatherworker = actor("LEATHERWORK"), alchemist = actor("ALCHEMY"), artisan = actor("ARTISAN"),
				cook = actor("COOKING");
		transactions.run(() -> {
			// 供应角色已有两把成品，但不提供任何工具或原料；本次缺额必须直接从库存交付。
			// 若错误地把现有库存加到制造目标，会触发额外制造并因缺少真实前置失败。
			journey.inventory.grantStrict(alchemist, "EXISTING_GOLD_NEEDLE", "TOOL_GOLD_NEEDLE", 2, false, false);
		});
		long buyerGold = journey.queries.find(leatherworker).gold();
		supply.supply(leatherworker, alchemist, artisan, cook, leatherworker, "TOOL_GOLD_NEEDLE", 1);
		assertEquals(1, quantity(leatherworker, "TOOL_GOLD_NEEDLE"));
		assertEquals(1, quantity(alchemist, "TOOL_GOLD_NEEDLE"), "已有库存只交付缺额，不应额外制造");
		assertEquals(buyerGold - 10, journey.queries.find(leatherworker).gold());
	}

	@Test
	void existingGathererStockDoesNotRequireAnotherGatheringTrip() {
		long leatherworker = actor("LEATHERWORK"), alchemist = actor("ALCHEMY"), artisan = actor("ARTISAN"),
				cook = actor("COOKING");
		transactions.run(() -> {
			// 明确不授予狩猎专业、地图资格或采集工具；已有的可交易兽皮应直接交付给缺料角色。
			journey.inventory.grantStrict(leatherworker, "EXISTING_GOLD_HIDE", "LIFE_HIDE_4", 2, false, false);
		});
		supply.supply(leatherworker, alchemist, artisan, cook, artisan, "LIFE_HIDE_4", 1);
		assertEquals(1, quantity(artisan, "LIFE_HIDE_4"));
		assertEquals(1, quantity(leatherworker, "LIFE_HIDE_4"));
	}

	@Test
	void goldNeedleUsesSilverToolAndIsOnlyDeliveredOnce() {
		long leatherworker = actor("LEATHERWORK"), alchemist = actor("ALCHEMY"), artisan = actor("ARTISAN"),
				cook = actor("COOKING");
		transactions.run(() -> {
			// 金针首把正式允许银档药杖启动，避免误要求先有金工具而形成循环。
			journey.inventory.grantStrict(alchemist, "SILVER_ROD", "TOOL_SILVER_ROD", 1, false, false);
			for (var ingredient : journey.production.recipe("TOOL_GOLD_NEEDLE").ingredients().entrySet())
				journey.inventory.grantStrict(alchemist, "NEEDLE_INPUT:" + ingredient.getKey(), ingredient.getKey(),
						ingredient.getValue(), false, false);
		});
		long before = journey.queries.find(leatherworker).gold();
		supply.supply(leatherworker, alchemist, artisan, cook, leatherworker, "TOOL_GOLD_NEEDLE", 1);
		assertEquals(1, quantity(leatherworker, "TOOL_GOLD_NEEDLE"));
		assertEquals(0, quantity(alchemist, "TOOL_GOLD_NEEDLE"));
		assertEquals(before - 10, journey.queries.find(leatherworker).gold());
		assertEquals(0, quantity(alchemist, "LIFE_RATION_3"));
		// 已经满足库存时不再次制造、不额外交付，也不重新扣款。
		supply.supply(leatherworker, alchemist, artisan, cook, leatherworker, "TOOL_GOLD_NEEDLE", 1);
		assertEquals(1, quantity(leatherworker, "TOOL_GOLD_NEEDLE"));
		assertEquals(before - 10, journey.queries.find(leatherworker).gold());
	}

	@Test
	void missingSilverRodIsManufacturedByLeatherworkerBeforeGoldNeedle() {
		long leatherworker = actor("LEATHERWORK"), alchemist = actor("ALCHEMY"), artisan = actor("ARTISAN"),
				cook = actor("COOKING");
		transactions.run(() -> {
			// 只提供前一档工具和两张配方的原料；银药杖必须由制皮角色真实制造并交付制药角色。
			journey.inventory.grantStrict(leatherworker, "IRON_NEEDLE", "TOOL_IRON_NEEDLE", 1, false, false);
			for (var ingredient : journey.production.recipe("TOOL_SILVER_ROD").ingredients().entrySet())
				journey.inventory.grantStrict(leatherworker, "ROD_INPUT:" + ingredient.getKey(), ingredient.getKey(),
						ingredient.getValue(), false, false);
			for (var ingredient : journey.production.recipe("TOOL_GOLD_NEEDLE").ingredients().entrySet())
				journey.inventory.grantStrict(alchemist, "NEEDLE_INPUT:" + ingredient.getKey(), ingredient.getKey(),
						ingredient.getValue(), false, false);
		});
		assertEquals(0, quantity(alchemist, "TOOL_SILVER_ROD"));
		supply.supply(leatherworker, alchemist, artisan, cook, leatherworker, "TOOL_GOLD_NEEDLE", 1);
		assertEquals(1, quantity(leatherworker, "TOOL_GOLD_NEEDLE"));
		assertEquals(0, quantity(leatherworker, "LIFE_RATION_2"), "补造银药杖必须消耗正式二级口粮");
		assertEquals(0, quantity(alchemist, "LIFE_RATION_3"), "制造金针必须消耗正式三级口粮");
	}

	@Test
	void artisanUsesOwnGoldTrapWithoutCreatingSelfPurchase() {
		long leatherworker = actor("LEATHERWORK"), alchemist = actor("ALCHEMY"), artisan = actor("ARTISAN"),
				cook = actor("COOKING");
		transactions.run(() -> {
			journey.inventory.grantStrict(artisan, "SILVER_HAMMER", "TOOL_SILVER_HAMMER", 1, false, false);
			for (var ingredient : journey.production.recipe("TOOL_GOLD_TRAP").ingredients().entrySet())
				journey.inventory.grantStrict(artisan, "HAMMER_INPUT:" + ingredient.getKey(), ingredient.getKey(),
						ingredient.getValue(), false, false);
		});
		long before = journey.queries.find(artisan).gold();
		supply.supply(leatherworker, alchemist, artisan, cook, artisan, "TOOL_GOLD_TRAP", 1);
		assertEquals(1, quantity(artisan, "TOOL_GOLD_TRAP"));
		assertEquals(before, journey.queries.find(artisan).gold(), "使用自己的产物不应发生市场扣费");
	}

	/** 夹具只提供本项回归的明确前置，不参加完整自然旅程的累计投入统计。 */
	private long actor(String skill) {
		String username = "late_supply_0" + UUID.randomUUID().toString().substring(0, 8);
		registrations.register(username, "Supply-test-1234", "测试");
		long owner = accounts.accountId(username);
		journey.characters.create(owner, "供料_0" + username.substring(username.length() - 8), "WARRIOR");
		transactions.run(() -> {
			journey.characters.grantExperience(owner, Math.toIntExact(growth.threshold(11)));
			journey.characters.changeGold(owner, 10000);
			facts.record(owner, "QUEST:O014");
			long character = journey.characters.lock(owner).id();
			jdbc.sql("UPDATE player_character SET location='CITY_SHOP' WHERE id=:id").param("id", character).update();
			skills.learn(character, LifeSkillRulesSkill.valueOf(skill));
			skills.progress(character, LifeSkillRulesSkill.valueOf(skill), LifeSkillRules.threshold(9) * 2);
		});
		return owner;
	}

	/** 聚合所有真实堆叠，确保跨账号交付结果不依赖服务端列表排序。 */
	private int quantity(long owner, String code) {
		return journey.items.list(owner)
			.stream()
			.filter(item -> item.code().equals(code))
			.mapToInt(item -> item.quantity())
			.sum();
	}

}
