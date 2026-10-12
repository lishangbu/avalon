package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.application.command.DungeonBattleCommandService;
import io.github.lishangbu.avalon.battle.application.query.DungeonBattleQueryService;
import io.github.lishangbu.avalon.battle.domain.*;
import io.github.lishangbu.avalon.battle.domain.BattleEngineResources;
import io.github.lishangbu.avalon.battle.domain.BattleStateOrder;
import io.github.lishangbu.avalon.battle.domain.BattleStateStats;
import io.github.lishangbu.avalon.battle.domain.BattleStateUnit;
import io.github.lishangbu.avalon.battle.domain.DungeonRoundStateSubmission;
import io.github.lishangbu.avalon.battle.persistence.repository.DungeonBattleRecordRepository;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;

/** 真实 PostgreSQL 验证副本行锁、提交凭证和跨领域资源回滚，不以序列化测试替代数据库证据。 */
class DungeonPersistenceTest extends SpringIntegrationTest {

	@Autowired
	EntityManager em;

	@Autowired
	DungeonBattleCommandService commands;

	@Autowired
	DungeonBattleQueryService queries;

	@Autowired
	DungeonBattleRecordRepository dungeonBattleRecordRepository;

	@Autowired
	DungeonBattlePolicy dungeon;

	@Autowired
	RegistrationService registration;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterQueryService characterQueries;

	private static final Instant NOW = Instant.parse("2030-01-01T00:00:00Z");

	private static final BattleEngineResources NO_ITEMS = new BattleEngineResources() {
		public int count(String code) {
			return 0;
		}

		public boolean room(BattleStateUnit target) {
			return false;
		}

		public void take(String code) {
			fail("不得消耗物品");
		}

		public void capture(BattleStateUnit target) {
			fail("不得捕捉");
		}
	};

	private record Fixture(long battle, List<Long> heroes, List<Long> owners) {
	}

	private Fixture fixture() {
		List<BattleStateUnit> party = new ArrayList<>();
		List<Long> owners = new ArrayList<>();
		List<Long> heroes = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			String name = "d_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
			registration.register(name, "Dungeon-database-123", "test");
			long owner = accounts.accountId(name);
			long character = Long.parseLong(characters.create(owner, name, "WARRIOR").id());
			owners.add(owner);
			heroes.add(character);
			var unit = new BattleStateUnit();
			unit.setId(Long.toString(character));
			unit.setName(name);
			unit.setHero(true);
			unit.setLevel(15);
			unit.setStats(new BattleStateStats(2000, 100, 100, 100, 100, 100, 50));
			unit.setHp(1800);
			unit.setMp(100);
			party.add(unit);
		}
		long battle = transactions.call(() -> {
			characters.changeGold(owners.getFirst(), 100);
			return commands.create(dungeon.create("D01_2", party, 31), NOW);
		});
		return new Fixture(battle, heroes, owners);
	}

	private DungeonRoundStateSubmission defend(long character) {
		String id = Long.toString(character);
		return new DungeonRoundStateSubmission(List.of(new BattleStateOrder(id, "DEFEND", id)), Map.of());
	}

	@Test
	void concurrentSubmissionsResolveOnceAndDurableReceiptsRejectChangedReplays() throws Exception {
		var f = fixture();
		try (var workers = Executors.newVirtualThreadPerTaskExecutor()) {
			var tasks = new ArrayList<Future<?>>();
			for (long hero : f.heroes()) {
				for (int retry = 0; retry < 2; retry++)
					tasks.add(workers.submit(() -> commands.submit(f.battle(), hero, 1, defend(hero), NOW, NO_ITEMS)));
			}
			for (var task : tasks)
				task.get(10, TimeUnit.SECONDS);
		}
		var restored = queries.restore(f.battle(), f.heroes().getFirst());
		assertEquals(2, restored.getBattle().getRound());
		assertEquals(0, restored.getBattle().getUnits().stream().filter(BattleStateUnit::isMechanism).count());
		assertEquals(3, receipts(f.battle()));
		commands.submit(f.battle(), f.heroes().getFirst(), 1, defend(f.heroes().getFirst()), NOW.plusSeconds(500),
				NO_ITEMS);
		assertEquals(2, queries.restore(f.battle(), f.heroes().getFirst()).getBattle().getRound());
		assertThrows(BusinessException.class, () -> commands.submit(f.battle(), f.heroes().getFirst(), 1,
				new DungeonRoundStateSubmission(
						List.of(new BattleStateOrder(Long.toString(f.heroes().getFirst()), "ATTACK", "dungeon-boss")),
						Map.of()),
				NOW, NO_ITEMS));
		assertThrows(BusinessException.class, () -> queries.restore(f.battle(), 1));
		assertThrows(BusinessException.class, () -> commands.submit(f.battle(), 1, 1, defend(1), NOW, NO_ITEMS));
	}

	@Test
	void concurrentExpiryRechecksDeadlineAndEventuallyStopsAbandonedBattle() throws Exception {
		var f = fixture();
		assertTrue(queries.due(NOW.plusSeconds(45)).contains(f.battle()));
		try (var workers = Executors.newVirtualThreadPerTaskExecutor()) {
			var a = workers.submit(() -> commands.expire(f.battle(), NOW.plusSeconds(45), NO_ITEMS));
			var b = workers.submit(() -> commands.expire(f.battle(), NOW.plusSeconds(45), NO_ITEMS));
			assertNotEquals(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS));
		}
		assertEquals(2, queries.restore(f.battle(), f.heroes().getFirst()).getBattle().getRound());
		assertFalse(queries.due(NOW.plusSeconds(45)).contains(f.battle()));
		assertThrows(BusinessException.class, () -> commands.submit(f.battle(), f.heroes().getFirst(), 1,
				defend(f.heroes().getFirst()), NOW.plusSeconds(46), NO_ITEMS));
		assertTrue(commands.expire(f.battle(), NOW.plusSeconds(90), NO_ITEMS));
		var restored = queries.restore(f.battle(), f.heroes().getFirst());
		assertEquals("LOST", restored.getBattle().getStatus());
		assertEquals(3, restored.getWithdrawn().size());
		assertFalse(commands.expire(f.battle(), NOW.plusSeconds(1000), NO_ITEMS));
		assertFalse(queries.due(NOW.plusSeconds(1000)).contains(f.battle()));
	}

	@Test
	void failedResolutionRollsBackReceiptBattleAndRelationalResourceTogether() {
		var f = fixture();
		long a = f.heroes().getFirst();
		var potion = new DungeonRoundStateSubmission(
				List.of(new BattleStateOrder(Long.toString(a), "HEALTH_POTION", Long.toString(a))), Map.of());
		var resources = new BattleEngineResources() {
			public int count(String code) {
				return 1;
			}

			public boolean room(BattleStateUnit target) {
				return false;
			}

			public void take(String code) {
				characters.changeGold(f.owners().getFirst(), -1);
			}

			public void capture(BattleStateUnit target) {
				fail("不能捕捉");
			}
		};
		commands.submit(f.battle(), a, 1, potion, NOW, resources);
		commands.submit(f.battle(), f.heroes().get(1), 1, defend(f.heroes().get(1)), NOW, resources);
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			commands.submit(f.battle(), f.heroes().get(2), 1, defend(f.heroes().get(2)), NOW, resources);
			throw new IllegalStateException("模拟后续进度结算失败");
		}));
		assertEquals(100, characterQueries.find(f.owners().getFirst()).gold());
		assertEquals(2, receipts(f.battle()));
		assertEquals(1, queries.restore(f.battle(), a).getBattle().getRound());
		commands.submit(f.battle(), f.heroes().get(2), 1, defend(f.heroes().get(2)), NOW, resources);
		assertEquals(99, characterQueries.find(f.owners().getFirst()).gold());
		assertEquals(3, receipts(f.battle()));
		assertEquals(2, queries.restore(f.battle(), a).getBattle().getRound());
		commands.submit(f.battle(), a, 1, potion, NOW, resources);
		assertEquals(99, characterQueries.find(f.owners().getFirst()).gold());
	}

	@Test
	void restoreDoesNotWaitForWriteLockOrMutateExpiredRounds() throws Exception {
		var f = fixture();
		var locked = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		try (var workers = Executors.newVirtualThreadPerTaskExecutor()) {
			var writer = workers.submit(() -> transactions.run(() -> {
				dungeonBattleRecordRepository.lock(f.battle());
				locked.countDown();
				try {
					if (!release.await(15, TimeUnit.SECONDS))
						throw new IllegalStateException("释放锁超时");
				}
				catch (InterruptedException error) {
					Thread.currentThread().interrupt();
					throw new IllegalStateException(error);
				}
			}));
			try {
				assertTrue(locked.await(5, TimeUnit.SECONDS));
				var read = workers.submit(() -> queries.restore(f.battle(), f.heroes().getFirst()));
				assertEquals(1, read.get(5, TimeUnit.SECONDS).getBattle().getRound());
			}
			finally {
				release.countDown();
			}
			writer.get(5, TimeUnit.SECONDS);
		}
		assertEquals(0, receipts(f.battle()));
	}

	private long receipts(long battle) {
		return transactions
			.call(() -> em.createQuery("select count(s) from DungeonSubmission s where battleId=:b", Long.class)
				.setParameter("b", battle)
				.getSingleResult());
	}

}
