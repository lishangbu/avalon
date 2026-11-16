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
