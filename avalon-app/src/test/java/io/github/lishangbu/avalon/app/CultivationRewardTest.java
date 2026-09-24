package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.content.battle.CultivationRewardCatalog;
import io.github.lishangbu.avalon.content.battle.GrowthRules;
import io.github.lishangbu.avalon.creature.application.command.EvolutionTeachingCommandService;
import io.github.lishangbu.avalon.creature.application.command.ResearchExchangeCommandService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.inventory.application.command.CultivationMaterialService.Mode;
import io.github.lishangbu.avalon.inventory.application.command.CultivationRewardService;
import io.github.lishangbu.avalon.inventory.application.command.CultivationRewardService.CompletedSource;
import io.github.lishangbu.avalon.inventory.application.command.EnhancementCommandService;
import io.github.lishangbu.avalon.inventory.application.command.EnhancementPreviewService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.inventory.domain.CultivationRewardRules;
import io.github.lishangbu.avalon.inventory.domain.CultivationRewardRules.Kind;
import io.github.lishangbu.avalon.inventory.domain.CultivationRewardRules.Stone;
import io.github.lishangbu.avalon.inventory.domain.EnhancementRoll;
import io.github.lishangbu.avalon.platform.BusinessException;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

/** 结算服务测试不代表具体高等级任务已开放；真实库存、凭证、锁与回滚，不替换发奖事务。 */
class CultivationRewardTest extends SpringIntegrationTest {

	@Autowired
	CultivationRewardCatalog catalog;

	@Autowired
	CultivationRewardService rewards;

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	InventoryCommandService inventory;

	@Autowired
	InventoryQueryService items;

	@Autowired
	EvolutionTeachingCommandService teaching;

	@Autowired
	ResearchExchangeCommandService exchange;

	@Autowired
	EnhancementPreviewService previews;

	@Autowired
	EnhancementCommandService enhancement;

	@Autowired
	EntityManager em;

	/** 无新增技能的进化路线只产出材料，不能因抽中整书概率而生成空编号物品。 */
	@Test
	void materialOnlyRouteNeverProducesAPhantomSkillBook() {
		var current = catalog.definition();
		var materialsOnly = new CultivationRewardCatalog.Definition(current.researchToken(), current.researchQuantity(),
				current.commissionStones(), current.dungeonStones(), current.stoneI(), current.stoneII(), 20, 10, 5,
				Map.of("CLAW_CAT", new CultivationRewardCatalog.Pool("WATER_PEARL", "TIDAL_CLAW_CAT_ESSENCE", null)));
		assertEquals(List.of(new CultivationRewardRules.Reward("WATER_PEARL", 1, false)),
				CultivationRewardRules.rewards(materialsOnly, Kind.REGIONAL_ELITE, "CLAW_CAT", null, 0, 0));
		assertTrue(
				CultivationRewardRules.rewards(materialsOnly, Kind.REGIONAL_ELITE, "CLAW_CAT", null, 99, 0).isEmpty());
	}

	/** 完整消费真实结算产物，证明来源绑定标记能贯穿兑换与强化，而不只是比较奖励DTO。 */
	@Test
	void completedSourcesFeedResearchExchangeAndBoundEnhancement() {
		TestDoubles.install(new GrowthRules() {
			@Override
			public int level(long experience) {
				return 30;
			}

			@Override
			public int carryingCapacity(int level) {
				return 8;
			}
		}, GrowthRules.class);
		TestDoubles.install(new EnhancementRoll() {
			@Override
			public int next() {
				return 99;
			}
		}, EnhancementRoll.class);
		try {
			long owner = owner();
			locate(owner, "CITY_PETS");
			teaching.learn(owner, new EvolutionTeachingCommandService.Request("GALE_SPARROW", true));
			for (int i = 0; i < 12; i++) {
				var event = new CompletedSource(Kind.RESEARCH_COMMISSION, "research-" + i, null, null, i);
				settle(owner, event);
				settle(owner, event);
			}
			var purchase = new ResearchExchangeCommandService.Request(UUID.randomUUID().toString(), "BOOK_GALE_BLADE",
					1, 12, true);
			exchange.exchange(owner, purchase);
			exchange.exchange(owner, purchase);
			assertTrue(items.list(owner).stream().noneMatch(i -> i.code().equals("RESEARCH_TOKEN")));
			var book = items.list(owner)
				.stream()
				.filter(i -> i.code().equals("BOOK_GALE_BLADE"))
				.findFirst()
				.orElseThrow();
			assertEquals(1, book.quantity());
			assertTrue(book.bound());
			locate(owner, "CITY_SHOP");
			transactions.run(() -> {
				characters.changeGold(owner, 100);
				inventory.grantStrict(owner, "GEAR", "STARTER_WEAPON", 1, true, false);
			});
			var event = new CompletedSource(Kind.ENHANCEMENT_DUNGEON, "run-source", null, Stone.I, 0);
			settle(owner, event);
			long id = Long
				.parseLong(items.list(owner).stream().filter(i -> i.slot() != null).findFirst().orElseThrow().id());
			assertThrows(BusinessException.class, () -> previews.preview(owner, id, null));
			var request = new EnhancementCommandService.Request(UUID.randomUUID().toString(),
					previews.preview(owner, id, Mode.BOUND_FIRST));
			var failed = enhancement.enhance(owner, id, request);
			assertFalse(failed.success());
			assertTrue(failed.item().bound());
			assertEquals(failed, enhancement.enhance(owner, id, request));
			settle(owner, event);
			assertEquals(5,
					items.list(owner)
						.stream()
						.filter(i -> i.code().equals("ENHANCEMENT_STONE_I"))
						.mapToInt(i -> i.quantity())
						.sum());
		}
		finally {
			TestDoubles.install(new GrowthRules(), GrowthRules.class);
			TestDoubles.install(new EnhancementRoll(), EnhancementRoll.class);
		}
	}

	private void locate(long owner, String location) {
		transactions.run(() -> {
			long id = characters.lock(owner).id();
			em.flush();
			em.clear();
			em.createNativeQuery("UPDATE player_character SET location=:location WHERE id=:id")
				.setParameter("location", location)
				.setParameter("id", id)
				.executeUpdate();
		});
	}

	private long owner() {
		String name = "yield_" + UUID.randomUUID().toString().substring(0, 8);
		registrations.register(name, "Reward-test-1234", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		return owner;
	}

	private CultivationRewardService.Result settle(long owner, CompletedSource source) {
		return transactions.call(() -> rewards.settle(owner, source));
	}

	@Test
	void exhaustiveIndependentPoolsAndStoneChoicesMatchApprovedBaseline() {
		var d = catalog.definition();
		for (String route : d.routes().keySet()) {
			int common = 0, special = 0, books = 0, both = 0, none = 0;
			var pool = d.routes().get(route);
			for (int material = 0; material < 100; material++)
				for (int book = 0; book < 100; book++) {
					var roll = CultivationRewardRules.rewards(d, Kind.REGIONAL_ELITE, route, null, material, book);
					if (roll.stream().anyMatch(r -> r.code().equals(pool.common())))
						common++;
					if (roll.stream().anyMatch(r -> r.code().equals(pool.special())))
						special++;
					if (roll.stream().anyMatch(r -> r.code().equals(pool.book())))
						books++;
					if (roll.size() == 2)
						both++;
					if (roll.isEmpty())
						none++;
					assertTrue(roll.stream().allMatch(r -> !r.bound() && r.quantity() == 1));
				}
			assertEquals(2000, common);
			assertEquals(1000, special);
			// 路线没有配置整书时只抽材料，不能为了满足旧断言给进化强加新招式。
			assertEquals(pool.book() == null ? 0 : 500, books);
			assertEquals(pool.book() == null ? 0 : 150, both);
			assertEquals(pool.book() == null ? 7000 : 6650, none);
		}
		for (var kind : new Kind[] { Kind.ENHANCEMENT_COMMISSION, Kind.ENHANCEMENT_DUNGEON }) {
			for (var stone : Stone.values()) {
				var reward = CultivationRewardRules.rewards(d, kind, null, stone, 0, 0).getFirst();
				assertEquals(kind == Kind.ENHANCEMENT_COMMISSION ? 3 : 6, reward.quantity());
				assertEquals(kind == Kind.ENHANCEMENT_COMMISSION || stone == Stone.I, reward.bound());
				assertEquals(stone == Stone.I ? d.stoneI() : d.stoneII(), reward.code());
			}
		}
		assertThrows(IllegalArgumentException.class,
				() -> CultivationRewardRules.rewards(d, Kind.REGIONAL_ELITE, "UNKNOWN", null, 0, 0));
		assertThrows(IllegalArgumentException.class,
				() -> CultivationRewardRules.rewards(d, Kind.ENHANCEMENT_DUNGEON, null, null, 0, 0));
	}

	@Test
	void participantReceiptFreezesChoiceAndRollbackRestoresWholeSettlement() {
		long first = owner(), second = owner();
		var source = new CompletedSource(Kind.ENHANCEMENT_DUNGEON, "run-1", null, Stone.II, 123);
		var result = settle(first, source);
		assertEquals(result, settle(first, source));
		assertEquals(result, settle(second, source), "同进度每名符合资格的成员独立发放");
		assertEquals(6, items.list(first).getFirst().quantity());
		assertFalse(items.list(first).getFirst().bound());
		assertThrows(BusinessException.class, () -> settle(first,
				new CompletedSource(source.kind(), source.eventId(), null, Stone.I, source.seed())));
		assertEquals(1, items.list(first).size());
		var research = new CompletedSource(Kind.RESEARCH_COMMISSION, "commission-1", null, null, 0);
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			rewards.settle(first, research);
			throw new IllegalStateException("模拟任务进度提交失败");
		}));
		assertTrue(items.list(first).stream().noneMatch(i -> i.code().equals("RESEARCH_TOKEN")));
		assertEquals(1, settle(first, research).rewards().getFirst().quantity());
		assertTrue(items.list(first).stream().filter(i -> i.code().equals("RESEARCH_TOKEN")).allMatch(i -> i.bound()));
	}

	@Test
	void concurrentDeliveryAndFullBagUseOnePendingReward() throws Exception {
		long owner = owner();
		transactions.run(() -> {
			for (int i = 0; i < 60; i++)
				inventory.grantStrict(owner, "FILL:" + i, "STARTER_WEAPON", 1, true, true);
		});
		var source = new CompletedSource(Kind.ENHANCEMENT_COMMISSION, "commission-2", null, Stone.I, 0);
		var start = new CountDownLatch(1);
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			var first = executor.submit(() -> {
				start.await();
				return settle(owner, source);
			});
			var second = executor.submit(() -> {
				start.await();
				return settle(owner, source);
			});
			start.countDown();
			assertEquals(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
		}
		assertEquals(60, items.list(owner).size());
		assertEquals(1, items.pending(owner).size());
		assertEquals(3, items.pending(owner).getFirst().quantity());
	}

	@Test
	void emptyEliteOutcomeStillFreezesEventAndSeed() {
		long owner = owner();
		long seed = 0;
		while (true) {
			var random = new SplittableRandom(seed);
			if (random.nextInt(100) >= 30 && random.nextInt(100) >= 5)
				break;
			seed++;
		}
		var source = new CompletedSource(Kind.REGIONAL_ELITE, "elite-1", "GALE_SPARROW", null, seed);
		assertTrue(settle(owner, source).rewards().isEmpty());
		assertEquals(settle(owner, source), settle(owner, source));
		assertThrows(BusinessException.class, () -> settle(owner,
				new CompletedSource(source.kind(), source.eventId(), source.route(), null, source.seed() + 1)));
		assertTrue(items.list(owner).isEmpty());
	}

}
