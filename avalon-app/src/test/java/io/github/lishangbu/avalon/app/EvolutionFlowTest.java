package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.persistence.repository.CombatVitalsStore;
import io.github.lishangbu.avalon.battle.persistence.repository.CombatVitalsRepository;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.battle.GrowthRules;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.creature.application.command.CreatureManagementService;
import io.github.lishangbu.avalon.creature.application.query.CreatureQueryService;
import io.github.lishangbu.avalon.market.application.command.MarketAccess;
import io.github.lishangbu.avalon.market.application.command.MarketListingCommandService;
import io.github.lishangbu.avalon.market.application.command.MarketPurchaseCommandService;
import io.github.lishangbu.avalon.market.application.query.MarketQueryService;
import io.github.lishangbu.avalon.creature.application.command.CreatureEvolutionCommandService;
import io.github.lishangbu.avalon.creature.application.command.CreatureEvolutionCommandService.Request;
import io.github.lishangbu.avalon.creature.application.command.EvolutionTeachingCommandService;
import io.github.lishangbu.avalon.creature.application.command.CreatureEvolutionPreviewService;
import io.github.lishangbu.avalon.creature.persistence.repository.CreatureRepository;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.inventory.application.command.CultivationMaterialService.Mode;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 使用真实20级伙伴与隔离的人物等级查询替身验证完整事务；不伪造已验收的16～30级经验曲线。 个体、材料、凭证、图鉴、资源及并发锁均使用实际数据库与正式应用服务。
 */
class EvolutionFlowTest extends SpringIntegrationTest {

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterQueryService characterQueries;

	@Autowired
	CreatureCommandService creatures;

	@Autowired
	CreatureRepository pets;

	@Autowired
	CreatureEvolutionPreviewService previews;

	@Autowired
	CreatureEvolutionCommandService evolution;

	@Autowired
	EvolutionTeachingCommandService teaching;

	@Autowired
	InventoryCommandService inventory;

	@Autowired
	InventoryQueryService items;

	@Autowired
	CombatVitalsStore combatVitalsStore;

	@Autowired
	CombatVitalsRepository combatVitalsRepository;

	@Autowired
	EntityManager em;

	@Autowired
	CreatureManagementService management;

	@Autowired
	CreatureQueryService creatureQueries;

	@Autowired
	MarketAccess marketAccess;

	@Autowired
	MarketListingCommandService listings;

	@Autowired
	MarketPurchaseCommandService purchases;

	@Autowired
	MarketQueryService marketQueries;

	@BeforeEach
	void levelFixture() {
		TestDoubles.install(new GrowthRules() {
			@Override
			public int level(long experience) {
				return 20;
			}

			@Override
			public int carryingCapacity(int level) {
				return 7;
			}
		}, GrowthRules.class);
	}

	@AfterEach
	void restoreGrowth() {
		TestDoubles.install(new GrowthRules(), GrowthRules.class);
	}

	private record Fixture(long owner, long character, long pet) {
	}

	private Fixture ready() {
		String name = "evo_" + UUID.randomUUID().toString().substring(0, 8);
		registrations.register(name, "Evolution-test-1234", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		var fixture = transactions.call(() -> {
			long character = characters.lock(owner).id();
			// 暗影箭不在新形态学习池，用于验证历史池外技能仍能完整保留，不能被进化重置。
			long pet = creatures.capture(owner, "FLOW", "GALE_SPARROW", 8, "H", "乘风", List.of("风刃", "逆风", "暗影箭"));
			var creature = pets.get(pet);
			creature.setLevel(20);
			creature.setExperience(19000); // 仅保留性测试样本，不作为正式20级经验配置。
			creature.setLocked(true);
			creature.setCustomName(true);
			creature.setName("小青");
			creature.setRenameCount(2);
			combatVitalsStore.saveVitals(character, pet, 0, 3);
			em.createNativeQuery("UPDATE player_character SET location = 'CITY_PETS' WHERE id = :id")
				.setParameter("id", character)
				.executeUpdate();
			return new Fixture(owner, character, pet);
		});
		teaching.learn(owner, new EvolutionTeachingCommandService.Request("GALE_SPARROW", true));
		return fixture;
	}

	@Test
	void boundMaterialsRequireExplicitChoiceAndEvolutionKeepsIndividualAndRetryResult() {
		var f = ready();
		assertThrows(BusinessException.class, () -> previews.preview(f.owner(), f.pet(), null));
		var quote = previews.preview(f.owner(), f.pet(), Mode.BOUND_FIRST);
		var request = new Request(UUID.randomUUID().toString(), quote);
		var result = evolution.evolve(f.owner(), f.pet(), request);
		assertEquals(result, evolution.evolve(f.owner(), f.pet(), request));
		assertTrue(result.bound());
		assertEquals("小青", result.name());
		assertEquals("GALE_FALCON", result.species());
		assertEquals(20, characterQueries.find(f.owner()).gold(), "原物种与新形态各奖励一次");
		assertFalse(items.list(f.owner())
			.stream()
			.anyMatch(item -> List.of("WIND_FEATHER", "GALE_PLUME").contains(item.code())));
		transactions.run(() -> {
			var pet = pets.get(f.pet());
			assertEquals(20, pet.getLevel());
			assertEquals(19000, pet.getExperience());
			assertEquals("H", pet.getAbilitySlot());
			assertEquals("乘风", pet.getAbility());
			assertTrue(pet.isLocked());
			assertEquals(2, pet.getRenameCount());
			assertEquals("[\"风刃\",\"逆风\",\"暗影箭\"]", pet.getSkills());
			assertEquals(f.character(), pet.getCaptorCharacterId());
			assertEquals(f.character(), pet.getOriginCharacterId());
			assertEquals(0, combatVitalsRepository.get(f.pet()).getHp());
			assertEquals(3, combatVitalsRepository.get(f.pet()).getMp());
			assertEquals(1L,
					((Number) em
						.createNativeQuery("SELECT count(*) FROM creature_evolution_receipt WHERE character_id=:id")
						.setParameter("id", f.character())
						.getSingleResult()).longValue());
		});
		assertThrows(BusinessException.class, () -> evolution.evolve(f.owner(), f.pet() + 1, request));
	}

	@Test
	void ownershipLevelAndStaleQuoteAreCheckedBeforeSpendingAndDefaultNameChanges() {
		var f = ready();
		var other = ready();
		assertThrows(BusinessException.class, () -> previews.preview(other.owner(), f.pet(), Mode.BOUND_FIRST));
		transactions.run(() -> pets.get(f.pet()).setLevel(15));
		assertThrows(BusinessException.class, () -> previews.preview(f.owner(), f.pet(), Mode.BOUND_FIRST));
		transactions.run(() -> {
			var pet = pets.get(f.pet());
			pet.setLevel(20);
			pet.setCustomName(false);
			pet.setName("青岚雀");
			pet.setBound(true);
		});
		var quote = previews.preview(f.owner(), f.pet(), null);
		assertEquals(Mode.BOUND_FIRST, quote.mode());
		var request = new Request(UUID.randomUUID().toString(), quote);
		transactions.run(() -> pets.get(f.pet()).setLocked(false));
		assertThrows(BusinessException.class, () -> evolution.evolve(f.owner(), f.pet(), request));
		assertEquals(2,
				items.list(f.owner())
					.stream()
					.filter(item -> List.of("WIND_FEATHER", "GALE_PLUME").contains(item.code()))
					.mapToInt(item -> item.quantity())
					.sum());
		var refreshed = previews.preview(f.owner(), f.pet(), null);
		transactions.run(() -> characters.setBattle(f.owner(), 123L, false));
		assertThrows(BusinessException.class,
				() -> evolution.evolve(f.owner(), f.pet(), new Request(UUID.randomUUID().toString(), refreshed)));
		transactions.run(() -> characters.setBattle(f.owner(), null, false));
		var result = evolution.evolve(f.owner(), f.pet(), new Request(UUID.randomUUID().toString(), refreshed));
		assertEquals("青岚隼", result.name());
		assertTrue(result.bound());
	}

	@Test
	void outerRollbackRestoresMaterialsShapeRewardsAndReceipt() {
		var f = ready();
		var quote = previews.preview(f.owner(), f.pet(), Mode.BOUND_FIRST);
		var request = new Request(UUID.randomUUID().toString(), quote);
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			evolution.evolve(f.owner(), f.pet(), request);
			throw new IllegalStateException("模拟最终提交失败");
		}));
		assertEquals(quote, previews.preview(f.owner(), f.pet(), Mode.BOUND_FIRST));
		assertEquals(10, characterQueries.find(f.owner()).gold());
		assertEquals("GALE_FALCON", evolution.evolve(f.owner(), f.pet(), request).species());
	}

	@Test
	void concurrentSameRequestConsumesUnboundSetOnce() throws Exception {
		var f = ready();
		transactions.run(() -> {
			inventory.grantStrict(f.owner(), "FREE_WIND", "WIND_FEATHER", 1, false, false);
			inventory.grantStrict(f.owner(), "FREE_PLUME", "GALE_PLUME", 1, false, false);
		});
		var quote = previews.preview(f.owner(), f.pet(), null);
		assertEquals(Mode.UNBOUND_ONLY, quote.mode());
		var request = new Request(UUID.randomUUID().toString(), quote);
		var start = new CountDownLatch(1);
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			var a = executor.submit(() -> {
				start.await();
				return evolution.evolve(f.owner(), f.pet(), request);
			});
			var b = executor.submit(() -> {
				start.await();
				return evolution.evolve(f.owner(), f.pet(), request);
			});
			start.countDown();
			var result = a.get(15, TimeUnit.SECONDS);
			assertEquals(result, b.get(15, TimeUnit.SECONDS));
			assertFalse(result.bound());
		}
		assertEquals(2,
				items.list(f.owner())
					.stream()
					.filter(item -> List.of("WIND_FEATHER", "GALE_PLUME").contains(item.code()))
					.mapToInt(item -> item.quantity())
					.sum(),
				"教学绑定材料仍保留");
		assertEquals(20, characterQueries.find(f.owner()).gold());
	}

	private void market(long owner) {
		transactions.run(() -> em.createNativeQuery("UPDATE player_character SET location='CITY_SHOP' WHERE id=:id")
			.setParameter("id", characters.lock(owner).id())
			.executeUpdate());
		marketAccess.teach(owner, true);
	}

	@Test
	void evolvedUnboundPetTradesWithPermanentDiscoveryAndResourceProvenance() {
		var f = ready();
		// 保留已收录物种的另一只可用伙伴，验证进化个体交易时不触发最后伙伴保护，也不额外改变图鉴金币。
		long retained = transactions
			.call(() -> creatures.capture(f.owner(), "WILD:RETAINED", "GALE_SPARROW", 8, "H", "乘风", List.of("风刃")));
		creatures.position(f.owner(), retained, 0);
		transactions.run(() -> {
			inventory.grantStrict(f.owner(), "TRADE_WIND", "WIND_FEATHER", 1, false, false);
			inventory.grantStrict(f.owner(), "TRADE_PLUME", "GALE_PLUME", 1, false, false);
		});
		evolution.evolve(f.owner(), f.pet(),
				new Request(UUID.randomUUID().toString(), previews.preview(f.owner(), f.pet(), null)));
		management.lock(f.owner(), f.pet(), false);
		creatures.position(f.owner(), f.pet(), 4);
		market(f.owner());
		String name = "evobuy_" + UUID.randomUUID().toString().substring(0, 7);
		registrations.register(name, "Evolution-test-1234", "测试");
		long buyer = accounts.accountId(name);
		characters.create(buyer, name, "WARRIOR");
		transactions.run(() -> characters.changeGold(buyer, 1000));
		market(buyer);
		var pet = creatureQueries.list(f.owner())
			.stream()
			.filter(p -> p.id().equals(Long.toString(f.pet())))
			.findFirst()
			.orElseThrow();
		var listed = listings.listCreature(f.owner(),
				new MarketListingCommandService.CreatureRequest(UUID.randomUUID().toString(), f.pet(), 100, pet));
		var detail = marketQueries.detail(buyer, Long.parseLong(listed.listingId())).creature();
		assertEquals("GALE_FALCON", detail.species());
		assertEquals("小青", detail.name());
		assertEquals(0, detail.resources().hp());
		var buy = new MarketPurchaseCommandService.Request(UUID.randomUUID().toString(),
				Long.parseLong(listed.listingId()), 1, 100);
		var received = purchases.buy(buyer, buy);
		assertEquals(received, purchases.buy(buyer, buy));
		assertEquals(910, characterQueries.find(buyer).gold(), "首次新形态奖励10金币且重试不重复领取");
		assertEquals(115, characterQueries.find(f.owner()).gold());
		var owned = creatureQueries.list(buyer).getFirst();
		assertEquals(pet.id(), owned.id());
		assertEquals(pet.skills(), owned.skills());
		assertEquals(pet.abilitySlot(), owned.abilitySlot());
		assertEquals(100, owned.partyPosition());
		assertFalse(creatureQueries.hasSourcePrefix(buyer, "FLOW"));
		assertTrue(creatureQueries.encyclopedia(f.owner())
			.stream()
			.filter(e -> e.species().equals("GALE_FALCON"))
			.findFirst()
			.orElseThrow()
			.collected());
		assertTrue(items.list(buyer).stream().anyMatch(i -> i.code().equals("SEAL_II") && i.bound()));
		transactions.run(() -> {
			assertEquals(f.character(), pets.get(f.pet()).getCaptorCharacterId());
			assertEquals(0, combatVitalsRepository.get(f.pet()).getHp());
			assertEquals(3, combatVitalsRepository.get(f.pet()).getMp());
			assertEquals(characters.lock(buyer).id(), combatVitalsRepository.get(f.pet()).getCharacterId());
		});
	}

	@Test
	void boundEvolutionCannotBeConvertedIntoTradablePet() {
		var f = ready();
		evolution.evolve(f.owner(), f.pet(),
				new Request(UUID.randomUUID().toString(), previews.preview(f.owner(), f.pet(), Mode.BOUND_FIRST)));
		management.lock(f.owner(), f.pet(), false);
		creatures.position(f.owner(), f.pet(), 4);
		market(f.owner());
		assertTrue(marketQueries.eligibleCreatures(f.owner()).isEmpty());
		assertThrows(BusinessException.class,
				() -> listings.listCreature(f.owner(), new MarketListingCommandService.CreatureRequest(
						UUID.randomUUID().toString(), f.pet(), 100, creatureQueries.list(f.owner()).getFirst())));
		assertEquals(1, creatureQueries.list(f.owner()).size());
	}

}
