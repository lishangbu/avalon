package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.application.command.DungeonRewardObserver;
import io.github.lishangbu.avalon.battle.application.command.DungeonRunCommandService;
import io.github.lishangbu.avalon.battle.application.model.DungeonEncounterFinished;
import io.github.lishangbu.avalon.battle.application.model.PartyFactory;
import io.github.lishangbu.avalon.battle.application.query.DungeonRunQueryService;
import io.github.lishangbu.avalon.battle.domain.BattleState;
import io.github.lishangbu.avalon.battle.domain.DungeonRewardRules;
import io.github.lishangbu.avalon.battle.domain.DungeonRoundState;
import io.github.lishangbu.avalon.battle.domain.DungeonRoundStateMember;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.command.CharacterFactCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.battle.DungeonCatalog;
import io.github.lishangbu.avalon.content.battle.DungeonRewardCatalog;
import io.github.lishangbu.avalon.content.battle.GrowthRules;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.content.quest.QuestDialogueCatalog;
import io.github.lishangbu.avalon.content.quest.QuestRulesCatalog;
import io.github.lishangbu.avalon.content.quest.model.QuestContentDefinition;
import io.github.lishangbu.avalon.content.quest.model.QuestContentStep;
import io.github.lishangbu.avalon.content.quest.model.QuestRepeatPolicy;
import io.github.lishangbu.avalon.content.quest.model.QuestRule;
import io.github.lishangbu.avalon.content.quest.model.QuestType;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.creature.application.query.CreatureQueryService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.inventory.application.query.DungeonChestQueryService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.world.application.command.QuestAcceptCommandService;
import io.github.lishangbu.avalon.world.application.model.QuestAcceptRequest;
import org.springframework.context.annotation.Import;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import io.github.lishangbu.avalon.content.quest.model.QuestPolicy;
import io.github.lishangbu.avalon.quest.model.*;
import io.github.lishangbu.avalon.content.quest.model.QuestReception;
import java.util.UUID;
import me.ahoo.cosid.IdGenerator;
import me.ahoo.cosid.provider.IdGeneratorProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;

/** 奖励使用真实存档和事务；战斗胜利事件由夹具构造，实际引擎接线另由 DungeonRunTest 覆盖。 */
@Import(QuestProgressFixture.class)
class DungeonRewardTest extends SpringIntegrationTest {

	@MockitoSpyBean
	QuestCatalog quests;

	@MockitoSpyBean
	QuestDialogueCatalog dialogues;

	/** 临时副本任务也必须具备正式生命周期规则。 */
	@MockitoSpyBean
	QuestRulesCatalog questRules;

	@Autowired
	QuestAcceptCommandService acceptance;

	@Autowired
	QuestProgressFixture objectives;

	@Autowired
	EntityManager entranceFixture;

	@Autowired
	GrowthRules growth;

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterFactCommandService facts;

	@Autowired
	CharacterQueryService characterQueries;

	@Autowired
	CreatureCommandService creatures;

	@Autowired
	CreatureQueryService pets;

	@Autowired
	InventoryQueryService items;

	@Autowired
	DungeonChestQueryService chests;

	@Autowired
	DungeonRunCommandService runs;

	@Autowired
	DungeonRunQueryService runQueries;

	@Autowired
	DungeonRewardObserver rewards;

	@Autowired
	DungeonRewardRules rules;

	@Autowired
	DungeonCatalog catalog;

	@Autowired
	DungeonRewardCatalog rewardCatalog;

	@Autowired
	CharacterFactQueryService factQueries;

	@Autowired
	PartyFactory parties;

	@Autowired
	@Qualifier(IdGeneratorProvider.SHARE + "SnowflakeId")
	IdGenerator ids;

	private long owner() {
		String name = "reward_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
		registrations.register(name, "Dungeon-test-1234", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		transactions.run(() -> {
			characters.grantExperience(owner, Math.toIntExact(growth.threshold(15)));
			facts.record(owner, "QUEST:BQ029");
			for (int i = 0; i < 5; i++)
				creatures.grant(owner, "REWARD-PET:" + i, "CLAW_CAT");
		});
		transactions.run(() -> entranceFixture
			.createNativeQuery("UPDATE player_character SET location='FOREST_PEN' WHERE account_id=:owner")
			.setParameter("owner", owner)
			.executeUpdate());
		return owner;
	}

	private DungeonEncounterFinished event(List<Long> owners, String stage) {
		long run = transactions.call(() -> runs.create(owners.getFirst(), owners, UUID.randomUUID().toString()));
		var state = runQueries.restore(run, owners.getFirst());
		var round = new DungeonRoundState();
		var battle = new BattleState();
		battle.setStatus("WON");
		battle.setDungeonStage(stage);
		battle.setSeed(719);
		battle.setUnits(new ArrayList<>());
		for (long owner : owners) {
			String hero = characterQueries.requireId(owner).toString();
			var member = new DungeonRoundStateMember();
			member.setParticipated(true);
			round.getMembers().put(hero, member);
			battle.getUnits().addAll(parties.create(owner, false));
		}
		round.setBattle(battle);
		return new DungeonEncounterFinished(run, ids.generate(), state, round);
	}

	private void settle(DungeonEncounterFinished event) {
		transactions.run(() -> rewards.reward(event));
	}

	@Test
	void firstExplorationRequiresEveryStageInTheSameRunAndRollsBackWithRewards() {
		var owners = List.of(owner(), owner(), owner());
		long leader = owners.getFirst(), missing = owners.getLast();
		String missedHero = characterQueries.requireId(missing).toString();
		var event = event(owners, "D01_1");
		String firstFact = "DUNGEON:FIRST_COMPLETE:D01";
		for (String stage : List.of("D01_1", "D01_2", "D01_3")) {
			event.result().getBattle().setDungeonStage(stage);
			event.result().getMembers().get(missedHero).setParticipated(!stage.equals("D01_2"));
			settle(new DungeonEncounterFinished(event.run(), ids.generate(), event.progress(), event.result()));
		}
		event.result().getMembers().get(missedHero).setParticipated(true);
		event.result().getBattle().setDungeonStage("D01_4");
		var finalEvent = new DungeonEncounterFinished(event.run(), ids.generate(), event.progress(), event.result());
		var before = pets.list(leader);
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			rewards.reward(finalEvent);
			throw new IllegalStateException("首次探索事务回滚");
		}));
		assertEquals(before, pets.list(leader));
		assertFalse(factQueries.codes(leader).contains(firstFact));
		settle(finalEvent);
		var after = pets.list(leader);
		settle(finalEvent);
		assertEquals(after, pets.list(leader));
		assertTrue(factQueries.codes(leader).contains(firstFact));
		assertTrue(factQueries.codes(owners.get(1)).contains(firstFact), "帮助者拥有独立首次探索资格");
		assertFalse(factQueries.codes(missing).contains(firstFact), "漏任一场不得消费首次资格");
		for (var pet : after) {
			var old = before.stream().filter(p -> p.id().equals(pet.id())).findFirst().orElseThrow();
			assertEquals(
					old.experience() + (pet.partyPosition() < 4
							? rules.experience(catalog.stage("D01_4"),
									finalEvent.result()
										.getBattle()
										.getUnits()
										.stream()
										.filter(unit -> unit.getId().equals(old.id()))
										.findFirst()
										.orElseThrow()
										.getLevel(),
									true) + rewardCatalog.definition().firstCompletionExperience()
							: 0),
					pet.experience());
		}
		// 换发起者重新开本：已经取得者不再领取，上一轮缺席者完整参与后才取得。
		owners.forEach(owner -> runs.leave(event.run(), owner));
		var next = event(List.of(missing, leader, owners.get(1)), "D01_1");
		for (var stage : catalog.definition().stages()) {
			next.result().getBattle().setDungeonStage(stage.id());
			settle(new DungeonEncounterFinished(next.run(), ids.generate(), next.progress(), next.result()));
		}
		assertTrue(factQueries.codes(missing).contains(firstFact));
	}

	@Test
	void victoryPaysLockedPetsAndDailyOnceAndReplayDoesNotDuplicate() {
		var owners = List.of(owner(), owner(), owner());
		long leader = owners.getFirst();
		long before = characterQueries.find(leader).gold();
		var beforePets = pets.list(leader);
		var event = event(owners, "D01_4");
		// 倒下不取消合法提交者资格，四只锁定伙伴得到经验，额外携带位不得补领。
		event.result().getBattle().getUnits().forEach(unit -> unit.setHp(0));
		settle(event);
		settle(event);
		assertEquals(1, chests.list(leader).size());
		assertTrue(chests.list(owners.get(1)).isEmpty());
		assertTrue(chests.list(owners.get(2)).isEmpty());
		assertEquals(before + 28, characterQueries.find(leader).gold());
		assertEquals(growth.threshold(15) + rules.experience(catalog.stage("D01_4"), 15, true),
				characterQueries.find(leader).experience());
		pets.list(leader).forEach(pet -> {
			var previous = beforePets.stream().filter(value -> value.id().equals(pet.id())).findFirst().orElseThrow();
			assertEquals(previous.experience()
					+ (pet.partyPosition() < 4 ? rules.experience(catalog.stage("D01_4"), previous.level(), true) : 0),
					pet.experience());
		});
		assertEquals(1,
				items.list(leader)
					.stream()
					.filter(item -> item.code().equals("MANA_POTION"))
					.mapToInt(item -> item.quantity())
					.sum());
		var second = new DungeonEncounterFinished(event.run(), ids.generate(), event.progress(), event.result());
		settle(second);
		assertEquals(1, chests.list(leader).size());
		assertEquals(before + 36, characterQueries.find(leader).gold());
		assertEquals(1,
				items.list(leader)
					.stream()
					.filter(item -> item.code().equals("MANA_POTION"))
					.mapToInt(item -> item.quantity())
					.sum());
	}

	@Test
	void ineligibleMembersAndLossReceiveNothing() {
		var owners = List.of(owner(), owner(), owner());
		var before = owners.stream().map(owner -> characterQueries.find(owner).gold()).toList();
		var event = event(owners, "D01_1");
		event.result().getMembers().get(characterQueries.requireId(owners.get(0)).toString()).setParticipated(false);
		event.result().getMembers().get(characterQueries.requireId(owners.get(1)).toString()).setRemoved(true);
		event.result().getMembers().get(characterQueries.requireId(owners.get(2)).toString()).setLeaving(true);
		settle(event);
		assertEquals(before, owners.stream().map(owner -> characterQueries.find(owner).gold()).toList());
		event.result().getMembers().values().forEach(member -> {
			member.setParticipated(true);
			member.setRemoved(false);
			member.setLeaving(false);
		});
		event.result().getBattle().setStatus("LOST");
		settle(event);
		assertEquals(before, owners.stream().map(owner -> characterQueries.find(owner).gold()).toList());
	}

	@Test
	void rewardFailureRollsBackCurrencyExperienceAndReceiptThenRetrySucceeds() {
		var owners = List.of(owner(), owner(), owner());
		long owner = owners.getFirst();
		long before = characterQueries.find(owner).gold();
		var beforePets = pets.list(owner);
		var event = event(owners, "D01_1");
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			rewards.reward(event);
			throw new IllegalStateException("模拟后续结算失败");
		}));
		assertEquals(before, characterQueries.find(owner).gold());
		assertEquals(beforePets, pets.list(owner));
		settle(event);
		settle(event);
		assertEquals(before + 12, characterQueries.find(owner).gold());
		pets.list(owner).forEach(pet -> {
			var previous = beforePets.stream().filter(value -> value.id().equals(pet.id())).findFirst().orElseThrow();
			assertEquals(previous.experience()
					+ (pet.partyPosition() < 4 ? rules.experience(catalog.stage("D01_1"), previous.level(), false) : 0),
					pet.experience());
		});
	}

	@Test
	void levelCorrectionAndResetBoundaryMatchConfirmedRules() {
		assertEquals(864, rules.experience(catalog.stage("D01_1"), 15, false));
		assertEquals(1131, rules.experience(catalog.stage("D01_1"), 1, false));
		assertEquals(560, rules.experience(catalog.stage("D01_4"), 100, true));
		assertEquals(LocalDate.of(2026, 9, 16), rules.day(Instant.parse("2026-09-16T20:59:59Z")));
		assertEquals(LocalDate.of(2026, 9, 17), rules.day(Instant.parse("2026-09-16T21:00:00Z")));
	}

	/** 真实任务参与状态与副本结算共同验证资格；事件夹具仅替代战斗计算，不替代任务或事务。 */
	@Test
	void onlyAcceptedActualParticipantsReceiveQuestProgressAndRollbackIsAtomic() {
		String stepId = "DUNGEON_PARTICIPATION_TEST";
		var task = new QuestContentStep(stepId, stepId, "兽栏协作", "FOREST_PEN", "交付", "完成", List.of(), true, List.of(),
				List.<String>copyOf(List.of("VICTORY:D01_1"))
					.stream()
					.map(code -> new QuestObjective(code,
							code.startsWith("ACTION:") || code.startsWith("SKILL:") ? QuestMeasure.HISTORY
									: QuestMeasure.EVENT,
							code, 1))
					.toList(),
				List.of(), QuestDelivery.MANUAL, null, null, null, true, List.of(),
				new QuestReception("FOREST_PEN", null));
		var steps = new ArrayList<>(quests.definition().steps());
		steps.add(task);
		doReturn(new QuestContentDefinition(steps)).when(quests).definition();
		doReturn(task).when(quests).step(stepId);
		doReturn(new QuestRule(stepId, "兽栏协作", QuestType.SIDE, List.of(), true,
				new QuestPolicy(QuestRepeatPolicy.ONCE, "Asia/Hong_Kong", 1, 10, null, false, List.of())))
			.when(questRules)
			.rule(stepId);
		doReturn(dialogues.step("BQ027")).when(dialogues).step(stepId);
		var owners = List.of(owner(), owner(), owner());
		long remote = owner();
		// 第一人正常参战，第二人进本但未行动，第三人参战但未接取；第四人留在副本外。
		for (long owner : List.of(owners.get(0), owners.get(1), remote))
			acceptance.accept(owner, stepId,
					new QuestAcceptRequest(UUID.randomUUID().toString(), characterQueries.find(owner).revision()));
		var event = event(owners, "D01_1");
		event.result().getMembers().get(characterQueries.requireId(owners.get(1)).toString()).setParticipated(false);
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			rewards.reward(event);
			throw new IllegalStateException("回滚副本结算");
		}));
		for (long owner : List.of(owners.get(0), owners.get(1), owners.get(2), remote))
			assertFalse(objectives.contains(characterQueries.requireId(owner), stepId, "VICTORY:D01_1"));
		settle(event);
		settle(event);
		assertEquals(1,
				objectives.goals(characterQueries.requireId(owners.getFirst()))
					.stream()
					.filter(value -> value.getStep().equals(stepId))
					.count());
		for (long owner : List.of(owners.get(1), owners.get(2), remote))
			assertFalse(objectives.contains(characterQueries.requireId(owner), stepId, "VICTORY:D01_1"));
	}

}
