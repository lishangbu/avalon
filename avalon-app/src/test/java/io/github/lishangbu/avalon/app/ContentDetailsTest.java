package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.content.battle.BattleCatalog;
import io.github.lishangbu.avalon.content.battle.CaptureBallCatalog;
import io.github.lishangbu.avalon.content.creature.CreatureCatalog;
import io.github.lishangbu.avalon.content.creature.AbilitySlotsCatalog;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.world.application.query.QuestRewardPreviewQueryService;
import io.github.lishangbu.avalon.world.application.query.QuestQueryService;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import java.util.UUID;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.creature.application.query.CreatureQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;

/** 说明必须读取真实目录参数，并明确处理不存在的对象，不能静默显示错误文案。 */
class ContentDetailsTest extends SpringIntegrationTest {

	@Test
	void foodAndToolDescriptionsIncludeTheirConfiguredNumbers() {
		assertTrue(details.detail("item", "FOOD_0_0").description().contains("恢复120生命"));
		assertTrue(details.detail("item", "FOOD_0_1").description().contains("恢复24魔力"));
		assertTrue(details.detail("item", "TOOL_WOOD_AXE").description().contains("覆盖生活技能1至2级"));
		assertTrue(details.detail("item", "TOOL_GOLD_AXE").description().contains("覆盖生活技能9至10级"));
	}

	@Autowired
	ContentDetailResource details;

	@Autowired
	BattleCatalog battles;

	@Autowired
	CaptureBallCatalog balls;

	@Autowired
	CreatureCatalog creatureCatalog;

	@Autowired
	AbilitySlotsCatalog abilitySlots;

	@Autowired
	QuestRewardPreviewQueryService rewards;

	@Autowired
	QuestQueryService quests;

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CreatureCommandService creatureCommands;

	@Autowired
	CreatureQueryService creatureQueries;

	@Test
	void individualStatsRequireOwnershipAndExposeSevenIntegers() {
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		var first = registrations.register("detail_a_" + suffix, "Details-test-1234", "测试");
		var second = registrations.register("detail_b_" + suffix, "Details-test-1234", "测试");
		long owner = accounts.accountId("detail_a_" + suffix);
		characters.create(owner, "图鉴甲" + suffix, "WARRIOR");
		characters.create(accounts.accountId("detail_b_" + suffix), "图鉴乙" + suffix, "MAGE");
		transactions.run(() -> creatureCommands.grant(owner, "DETAIL_TEST", "CLAW_CAT"));
		String id = creatureQueries.list(owner).getFirst().id();
		client.get()
			.uri("/api/content/details/creature/" + id)
			.header("Authorization", "Bearer " + first.accessToken())
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.expectBody()
			.jsonPath("$.rules.length()")
			.isEqualTo(7);
		client.get()
			.uri("/api/content/details/creature/" + id)
			.header("Authorization", "Bearer " + second.accessToken())
			.exchange()
			.expectStatus()
			.isEqualTo(404);
	}

	@Test
	void readingKnownRewardDoesNotAcceptAndFutureRewardsStayHidden() {
		// 测试账号使用数字隔开前缀和随机串，避免严格短词在拼接处随机命中。
		String username = "details_0" + UUID.randomUUID().toString().substring(0, 8);
		registrations.register(username, "Details-test-1234", "测试");
		long owner = accounts.accountId(username);
		characters.create(owner, "说明测试", "WARRIOR");
		var before = quests.state(owner);
		assertTrue(rewards.items(owner, "BQ001").isEmpty());
		assertEquals(before, quests.state(owner));
		assertThrows(BusinessException.class, () -> rewards.items(owner, "BQ066"));
	}

	@Test
	void skillAndBallDetailsUseCurrentConfiguration() {
		var skill = battles.skill("烈焰冲击");
		var view = details.detail("skill", skill.id());
		assertTrue(view.rules().contains("消耗魔力：" + skill.mana()));
		assertTrue(view.rules().contains("威力：" + skill.power()));
		assertTrue(
				view.rules().stream().anyMatch(text -> text.contains(String.valueOf(skill.lowHealthPower().power()))));
		var ball = balls.find("SEAL_II");
		assertTrue(details.detail("item", ball.code()).rules().contains("封印球捕捉倍率：" + ball.multiplier()));
		assertTrue(details.detail("item", "BOOK_WATER").rules().stream().anyMatch(text -> text.contains("水弹")));
	}

	@Test
	void abilityAndSpeciesDescriptionsAreSpecificAndUnknownIdsAreRejected() {
		assertTrue(creatureCatalog.pet("CLAW_CAT").description().contains("溪边"));
		assertNotEquals(creatureCatalog.pet("CLAW_CAT").description(),
				creatureCatalog.pet("STREAM_CLAW_CAT").description());
		assertEquals(abilitySlots.description("灵爪"), details.detail("ability", "灵爪").description());
		assertThrows(BusinessException.class, () -> details.detail("item", "UNKNOWN"));
		assertThrows(BusinessException.class, () -> details.detail("ability", "UNKNOWN"));
		assertThrows(BusinessException.class, () -> details.detail("species", "CLAW_CAT"));
	}

}
