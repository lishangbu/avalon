package io.github.lishangbu.avalon.app;

import org.assertj.core.api.Assertions;
import java.util.List;
import static org.hamcrest.MatcherAssert.assertThat;

import io.github.lishangbu.avalon.battle.application.query.BattleQueryService;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.command.CharacterFactCommandService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;

import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/** 验证角色档案走真实认证、装备和训练记录，避免界面展示固定初始属性。 */
class CharacterCombatProfileTest extends SpringIntegrationTest {

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterFactCommandService facts;

	@Autowired
	InventoryCommandService inventory;

	@Autowired
	InventoryQueryService items;

	@Autowired
	BattleQueryService battles;

	@Test
	void profileReflectsEquipmentAndLearnedSkillsWithoutRestoringResources() {
		String name = "profile_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
		var tokens = registrations.register(name, "Profile-test-1234", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		client.get()
			.uri("/api/battle/character-profile")
			.headers(headers -> headers.setBearerAuth(tokens.accessToken()))
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.expectBody()
			.jsonPath("$.stats.health")
			.isEqualTo(176)
			.jsonPath("$.stats.mana")
			.isEqualTo(33)
			.jsonPath("$.skills[*].name")
			.value(List.class, actual -> Assertions.assertThat(actual).containsExactly("英勇打击", "援护"));
		transactions.run(() -> {
			inventory.grant(owner, "PROFILE", "STARTER_BODY", 1, true);
			facts.record(owner, "TRAINING:BQ023");
		});
		long body = Long.parseLong(items.list(owner).getFirst().id());
		inventory.equip(owner, body);
		var vitals = battles.lobby(owner).vitals();
		var profile = battles.characterProfile(owner);
		assertEquals(206, profile.stats().health());
		assertEquals("盾墙", profile.skills().getLast().name());
		assertEquals(12, profile.skills().getLast().mana());
		assertEquals(4, profile.skills().getLast().cooldown());
		assertEquals(profile, battles.characterProfile(owner));
		assertEquals(vitals, battles.lobby(owner).vitals(), "重复读取不能补充装备增加的生命上限");
		assertNull(battles.current(owner), "读档案不创建战局");
		inventory.unequip(owner, body);
		assertEquals(176, battles.characterProfile(owner).stats().health());
		client.get().uri("/api/battle/character-profile").exchange().expectStatus().isEqualTo(401);
	}

}
