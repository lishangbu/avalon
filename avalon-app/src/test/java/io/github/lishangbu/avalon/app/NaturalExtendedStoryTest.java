package io.github.lishangbu.avalon.app;

import static org.junit.jupiter.api.Assertions.assertTrue;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.world.application.query.QuestQueryService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import java.time.Instant;
import static org.mockito.Mockito.doAnswer;

/** 新故事自然路线回归：验证开篇真实任务、战斗、资源和双城分支。 */
@Import(StoryOpeningJourney.class)
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

	@ParameterizedTest
	@ValueSource(strings = { "WARRIOR", "MAGE", "PRIEST", "RANGER" })
	void naturalOpeningReachesSealAndRecordsCompletion(String profession) {
		long owner = opening(profession);
		assertTrue(quests.done(owner, "N025"));
	}

	@Test
	void naturalWarriorCanChooseBothCityBranches() {
		long owner = opening("WARRIOR");
		journey.completeCityApproach(owner, "U");
		journey.completeCityApproach(owner, "S");
		assertTrue(quests.done(owner, "U005"));
		assertTrue(quests.done(owner, "S005"));
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
		return owner;
	}

}
