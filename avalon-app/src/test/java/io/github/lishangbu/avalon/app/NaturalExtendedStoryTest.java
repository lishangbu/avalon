package io.github.lishangbu.avalon.app;

import static org.junit.jupiter.api.Assertions.assertTrue;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.world.application.query.QuestQueryService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import java.time.Instant;
import static org.mockito.Mockito.doAnswer;

/** 新故事自然路线回归：验证开篇真实任务、战斗、资源和双城分支。 */
@Import(StoryOpeningJourney.class)
@Timeout(12 * 60)
class NaturalExtendedStoryTest extends SpringIntegrationTest {

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	StoryOpeningJourney journey;

	@Autowired
	QuestQueryService quests;

	@MockitoSpyBean
	io.github.lishangbu.avalon.world.domain.LifeClock lifeClock;

	@org.springframework.test.context.bean.override.mockito.MockitoBean
	io.github.lishangbu.avalon.battle.domain.BattleSeedSource seeds;

	@org.springframework.test.context.bean.override.mockito.MockitoBean
	io.github.lishangbu.avalon.world.domain.GatheringYieldRoll gatheringYield;

	@org.springframework.test.context.bean.override.mockito.MockitoBean
	io.github.lishangbu.avalon.world.domain.GatheringRareRoll gatheringRare;

	@org.springframework.test.context.bean.override.mockito.MockitoBean
	io.github.lishangbu.avalon.world.domain.LifeToolRandom toolRandom;

	@org.junit.jupiter.api.BeforeEach
	void seedJourney(org.junit.jupiter.api.TestInfo test) {
		long seed = Long.getLong("avalon.test.seed", 20261025L);
		var random = new java.util.Random(seed ^ test.getDisplayName().hashCode());
		org.mockito.Mockito.when(seeds.nextLong()).thenAnswer(call -> random.nextLong());
		var gathering = new java.util.Random(seed ^ test.getDisplayName().hashCode() ^ 0x1234ABCDL);
		org.mockito.Mockito
			.when(gatheringYield.succeeds(org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
			.thenAnswer(call -> gathering.nextInt(100) < io.github.lishangbu.avalon.world.domain.GatheringYieldRoll
				.chance(call.getArgument(0), call.getArgument(1)));
		org.mockito.Mockito
			.when(gatheringRare.succeeds(org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
			.thenAnswer(call -> gathering.nextInt(100) < io.github.lishangbu.avalon.world.domain.GatheringRareRoll
				.chance(call.getArgument(0), call.getArgument(1)));
		org.mockito.Mockito.when(toolRandom.breaks(org.mockito.ArgumentMatchers.anyInt()))
			.thenAnswer(call -> gathering.nextInt(call.getArgument(0, Integer.class)) == 0);
		System.out.printf("STORY_SEED test=%s seed=%d%n", test.getDisplayName(), seed);
	}

	@ParameterizedTest
	@ValueSource(strings = { "U", "S" })
	void naturalCitySupplySideUsesGatheringAndFreshCrafting(String prefix) {
		long owner = opening("WARRIOR");
		journey.completeCityApproach(owner, prefix);
		journey.completeSupplySide(owner, prefix);
		for (int number = 26; number <= 30; number++)
			assertTrue(quests.done(owner, "%s%03d".formatted(prefix, number)));
		journey.completeCityPreparation(owner, prefix, true);
		journey.completeCityForecourt(owner, prefix);
		journey.completeCitySeal(owner, prefix);
		journey.completeBountySide(owner, prefix);
		for (int number = 51; number <= 55; number++)
			assertTrue(quests.done(owner, "%s%03d".formatted(prefix, number)));
		if (prefix.equals("S")) {
			journey.completeSylphCampSide(owner);
			assertTrue(quests.done(owner, "S045"));
		}
	}

	@Test
	void naturalSylphFirstReachesFireCompletion() {
		citiesAndFire("S");
	}

	@Test
	void naturalUndineFirstReachesJointCompletion() {
		long owner = citiesAndFire("U");
		journey.completeJoint(owner);
		journey.completeResidual(owner);
		journey.completeCityCollections(owner);
	}

	@ParameterizedTest
	@ValueSource(strings = { "MAGE", "PRIEST", "RANGER" })
	void otherProfessionsNaturallyReachJointCompletion(String profession) {
		long owner = citiesAndFire("U", profession);
		journey.completeJoint(owner);
		assertTrue(quests.done(owner, "J030"));
	}

	private long citiesAndFire(String first) {
		return citiesAndFire(first, "WARRIOR");
	}

	private long citiesAndFire(String first, String profession) {
		long owner = opening(profession);
		journey.prepareWaterPartner(owner);
		for (String prefix : java.util.List.of(first, first.equals("U") ? "S" : "U")) {
			journey.completeCityApproach(owner, prefix);
			assertTrue(quests.done(owner, prefix + "005"));
			journey.completeCityPreparation(owner, prefix, prefix.equals(first));
			journey.completeCityForecourt(owner, prefix);
			journey.completeCitySeal(owner, prefix);
		}
		assertTrue(quests.done(owner, "U025"));
		assertTrue(quests.done(owner, "S025"));
		journey.completeFire(owner, first.equals("S"));
		assertTrue(quests.done(owner, "F025"));
		return owner;
	}

	private long opening(String profession) {
		final Instant[] now = { Instant.now() };
		doAnswer(invocation -> {
			now[0] = now[0].plusSeconds(1);
			return now[0];
		}).when(lifeClock).now();
		String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
		String username = "natural_story_" + suffix;
		registrations.register(username, "Story-journey-123", "新故事自然路线");
		long owner = accounts.accountId(username);
		characters.create(owner, "自然路线" + suffix, profession);
		journey.complete(owner);
		journey.completeMineApproach(owner);
		journey.completeAbandonedPost(owner);
		journey.completeInnerMine(owner);
		journey.completeUndergroundApproach(owner);
		journey.completeSealDungeon(owner);
		assertTrue(quests.done(owner, "N025"));
		return owner;
	}

}
