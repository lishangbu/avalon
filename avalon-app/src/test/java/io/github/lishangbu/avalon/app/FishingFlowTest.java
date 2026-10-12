package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.life.LifeSkillRulesSkill;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.world.application.command.FishingCommandService;
import io.github.lishangbu.avalon.world.application.command.LifeSkillCommandService;
import io.github.lishangbu.avalon.world.application.model.FishingInput;
import io.github.lishangbu.avalon.world.application.model.FishingSession;
import io.github.lishangbu.avalon.world.application.model.FishingStart;
import io.github.lishangbu.avalon.world.application.model.LifeSkillRequest;
import io.github.lishangbu.avalon.world.domain.FishingRules;
import io.github.lishangbu.avalon.world.domain.LifeClock;
import io.github.lishangbu.avalon.world.domain.LifeToolRandom;
import io.github.lishangbu.avalon.world.persistence.repository.LifeSkillRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class FishingFlowTest extends SpringIntegrationTest {

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterQueryService heroes;

	@Autowired
	InventoryCommandService inventory;

	@Autowired
	InventoryQueryService bag;

	@Autowired
	LifeSkillCommandService skills;

	@Autowired
	LifeSkillRepository learned;

	@Autowired
	FishingCommandService fishing;

	@Autowired
	JdbcClient jdbc;

	@MockitoBean
	LifeClock clock;

	@MockitoBean
	LifeToolRandom random;

	private final Instant start = Instant.parse("2026-09-22T00:00:00Z");

	private long owner() {
		when(clock.now()).thenReturn(start);
		String name = "fish_" + UUID.randomUUID().toString().substring(0, 8);
		registrations.register(name, "Fish-test-12345", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		transactions.run(() -> {
			characters.grantExperience(owner, 7380);
			inventory.grant(owner, "BAIT", "FISH_BAIT", 3, false);
			jdbc.sql("UPDATE player_character SET location='RIVERSIDE' WHERE id=:id")
				.param("id", characters.lock(owner).id())
				.update();
		});
		skills.change(owner, new LifeSkillRequest(UUID.randomUUID().toString(), LifeSkillRulesSkill.FISHING, false),
				false);
		return owner;
	}

	private String item(long owner, String code) {
		return bag.list(owner).stream().filter(value -> value.code().equals(code)).findFirst().orElseThrow().id();
	}

	private int count(long owner, String code) {
		return bag.list(owner)
			.stream()
			.filter(value -> value.code().equals(code))
			.mapToInt(value -> value.quantity())
			.sum();
	}

	private FishingStart request(long owner) {
		return new FishingStart(UUID.randomUUID().toString(), "FISH_0_0", item(owner, "TOOL_WOOD_FISHING_ROD"),
				item(owner, "FISH_BAIT"));
	}

	private FishingSession track(long owner, FishingSession state) {
		for (int time = 0; time <= 20000 && state.state().status().equals("PLAYING"); time += 100) {
			when(clock.now()).thenReturn(start.plusMillis(time));
			boolean held = state.state().position() < FishingRules.fishPosition(state.seed(), 1,
					Math.min(time + 100, 20000));
			state = fishing.input(owner, new FishingInput(state.id(), state.revision(), held, true));
		}
		return state;
	}

	@Test
	void successfulCatchConsumesBaitOnceAndBreaksRodOnlyAfterReward() {
		long owner = owner();
		var request = request(owner);
		var session = fishing.start(owner, request);
		assertEquals(session, fishing.start(owner, request));
		assertEquals(2, count(owner, "FISH_BAIT"));
		assertThrows(BusinessException.class, () -> transactions.run(() -> characters.requireNoLifeActivity(owner)));
		when(random.breaks(anyInt())).thenReturn(true);
		var result = track(owner, session);
		assertEquals("CAUGHT", result.state().status());
		assertTrue(result.broken());
		assertEquals(1, count(owner, "FISH_0_0"));
		assertTrue(bag.list(owner)
			.stream()
			.filter(value -> value.code().equals("FISH_0_0"))
			.allMatch(value -> value.bound()));
		assertEquals(0, count(owner, "TOOL_WOOD_FISHING_ROD"));
		assertEquals(4L, learned.owned(heroes.requireId(owner)).get(LifeSkillRulesSkill.FISHING));
		assertEquals(result, fishing.input(owner,
				new FishingInput(result.id(), result.revision() - 1, result.lastHeld(), result.lastForeground())));
		assertEquals(1, count(owner, "FISH_0_0"));
		transactions.run(() -> characters.requireNoLifeActivity(owner));
		verify(random, times(1)).breaks(100);
	}

	@Test
	void disconnectLosesOnlyBaitAndOldSessionCannotCancelNewOne() {
		long owner = owner(), other = owner();
		var state = fishing.start(owner, request(owner));
		assertThrows(BusinessException.class,
				() -> fishing.input(other, new FishingInput(state.id(), 0, false, false)));
		when(clock.now()).thenReturn(start.plusSeconds(60));
		var lost = fishing.input(owner, new FishingInput(state.id(), 0, false, true));
		assertEquals("ESCAPED", lost.state().status());
		assertEquals(0, count(owner, "FISH_0_0"));
		assertEquals(1, count(owner, "TOOL_WOOD_FISHING_ROD"));
		assertEquals(2, count(owner, "FISH_BAIT"));
		fishing.start(owner, request(owner));
		assertThrows(BusinessException.class,
				() -> fishing.input(owner, new FishingInput(state.id(), lost.revision(), false, false)));
		assertThrows(BusinessException.class, () -> transactions.run(() -> characters.requireNoLifeActivity(owner)));
		verifyNoInteractions(random);
	}

	@Test
	void fullBagDoesNotAwardFishOrProficiencyOrBreakRod() {
		long owner = owner();
		transactions.run(() -> {
			for (int index = 0; index < 58; index++)
				inventory.grantStrict(owner, "FILL:" + index, "STARTER_WEAPON", 1, true, false);
		});
		var result = track(owner, fishing.start(owner, request(owner)));
		assertEquals("BAG_FULL", result.state().status());
		assertEquals(0, count(owner, "FISH_0_0"));
		assertEquals(0L, learned.owned(heroes.requireId(owner)).get(LifeSkillRulesSkill.FISHING));
		assertEquals(1, count(owner, "TOOL_WOOD_FISHING_ROD"));
		transactions.run(() -> characters.requireNoLifeActivity(owner));
		verifyNoInteractions(random);
	}

}
