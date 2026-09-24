package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.content.life.LifeSkillRules;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.creature.application.query.CreatureQueryService;
import io.github.lishangbu.avalon.world.application.command.LifeSkillCommandService;
import io.github.lishangbu.avalon.world.application.command.LifeSkillCommandService.Request;
import io.github.lishangbu.avalon.content.life.LifeSkillRules.Skill;
import io.github.lishangbu.avalon.world.persistence.repository.LifeSkillRepository;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.http.MediaType;
import static org.junit.jupiter.api.Assertions.*;

class LifeSkillFlowTest extends SpringIntegrationTest {

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterQueryService queries;

	@Autowired
	LifeSkillCommandService commands;

	@Autowired
	LifeSkillRepository skills;

	@Autowired
	JdbcClient jdbc;

	@Autowired
	CreatureCommandService creatures;

	@Autowired
	CreatureQueryService pets;

	@Test
	void fiftyLevelGrowthPersistsForHeroAndCompanion() {
		long owner = ready(true);
		transactions.run(() -> {
			creatures.grant(owner, "LIFE_GROWTH", "SPROUT");
			characters.grantExperience(owner, Integer.MAX_VALUE);
			creatures.grantPartyExperience(owner, Integer.MAX_VALUE);
		});
		assertEquals(50, queries.find(owner).level());
		assertEquals(23935620, queries.find(owner).experience());
		assertEquals(50, pets.list(owner).getFirst().level());
	}

	@Test
	void httpCommandsReturnNoContentAndOnlyExposeAuthenticatedSkills() {
		client.get().uri("/api/life/skills").exchange().expectStatus().isEqualTo(401);
		String name = "life_" + UUID.randomUUID().toString().substring(0, 8);
		String token = registrations.register(name, "Life-test-12345", "测试").accessToken();
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		transactions.run(() -> characters.grantExperience(owner, 7380));
		var request = request(Skill.MINING);
		for (int attempt = 0; attempt < 2; attempt++)
			client.post()
				.uri("/api/life/skills/learn")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.body(request)
				.exchange()
				.expectStatus()
				.isEqualTo(204);
		client.get()
			.uri("/api/life/skills")
			.header("Authorization", "Bearer " + token)
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("$[0].learned")
			.isEqualTo(true);
	}

	private long ready(boolean unlocked) {
		String name = "life_" + UUID.randomUUID().toString().substring(0, 8);
		registrations.register(name, "Life-test-12345", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		if (unlocked)
			jdbc.sql("UPDATE player_character SET experience=20000 WHERE id=:id")
				.param("id", queries.requireId(owner))
				.update();
		return owner;
	}

	private Request request(Skill skill) {
		return new Request(UUID.randomUUID().toString(), skill, true);
	}

	@Test
	void eligibilityConfirmationAndForgetReplayProtectRelearnedProgress() {
		long locked = ready(false);
		assertThrows(BusinessException.class, () -> commands.change(locked, request(Skill.MINING), false));
		long owner = ready(true);
		long eligible = owner;
		commands.change(owner, request(Skill.MINING), false);
		var forget = request(Skill.MINING);
		assertThrows(BusinessException.class,
				() -> commands.change(eligible, new Request(forget.requestId(), Skill.MINING, false), true));
		commands.change(owner, forget, true);
		commands.change(owner, request(Skill.MINING), false);
		long id = queries.requireId(owner);
		transactions.run(() -> {
			characters.lock(eligible);
			skills.progress(id, Skill.MINING, 257);
		});
		commands.change(owner, forget, true);
		assertEquals(257L, skills.owned(id).get(Skill.MINING));
		assertThrows(BusinessException.class,
				() -> commands.change(eligible, new Request(forget.requestId(), Skill.LOGGING, true), true));
		assertTrue(skills.owned(queries.requireId(ready(true))).isEmpty());
	}

	@Test
	void concurrentFourthProfessionRejectedWhileFarmingAndFishingRemainAvailable() throws Exception {
		long owner = ready(true);
		commands.change(owner, request(Skill.MINING), false);
		commands.change(owner, request(Skill.LOGGING), false);
		var start = new CountDownLatch(1);
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			var attempts = List.of(Skill.HUNTING, Skill.ALCHEMY).stream().map(skill -> executor.submit(() -> {
				assertTrue(start.await(10, TimeUnit.SECONDS));
				try {
					commands.change(owner, request(skill), false);
					return true;
				}
				catch (BusinessException error) {
					return false;
				}
			})).toList();
			start.countDown();
			int successes = 0;
			for (var attempt : attempts)
				if (attempt.get(20, TimeUnit.SECONDS))
					successes++;
			assertEquals(1, successes);
		}
		commands.change(owner, request(Skill.FARMING), false);
		commands.change(owner, request(Skill.FISHING), false);
		assertEquals(5, skills.owned(queries.requireId(owner)).size());
	}

}
