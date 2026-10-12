package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.creature.application.command.CreatureNamingCommandService;
import io.github.lishangbu.avalon.creature.application.model.CreatureNamingRequest;
import io.github.lishangbu.avalon.creature.application.query.CreatureNamingQueryService;
import io.github.lishangbu.avalon.creature.application.query.CreatureQueryService;
import io.github.lishangbu.avalon.creature.persistence.entity.OwnedCreature;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;

/** 来源元数据随创建事务提交；改名不能改捕获者，外层失败不能留下半个可交易个体。 */
class CreatureProvenanceTest extends SpringIntegrationTest {

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CreatureCommandService creatures;

	@Autowired
	CreatureNamingCommandService naming;

	@Autowired
	CreatureNamingQueryService names;

	@Autowired
	CreatureQueryService queries;

	@Autowired
	EntityManager em;

	private long player() {
		String name = "origin_" + UUID.randomUUID().toString().substring(0, 8);
		registrations.register(name, "Origin-test-1234", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		return owner;
	}

	@Test
	void giftsAndHiddenCapturesKeepDifferentProvenanceAfterRename() {
		long owner = player();
		long id = transactions.call(() -> {
			creatures.grant(owner, "ORIGIN_GIFT", "SPROUT");
			return creatures.capture(owner, "ORIGIN_CAPTURE", "STONE_GOLEM", 1, "H", "守护", List.of("碎岩击"));
		});
		transactions.run(() -> {
			var captured = em.find(OwnedCreature.class, id);
			assertEquals(captured.getCharacterId(), captured.getCaptorCharacterId());
			assertEquals(captured.getCharacterId(), captured.getOriginCharacterId());
			assertEquals("CAPTURE", captured.getOriginKind());
			assertFalse(captured.isBound());
			assertFalse(captured.isCustomName());
			var gift = em
				.createQuery("from OwnedCreature where characterId = :owner and source = 'ORIGIN_GIFT'",
						OwnedCreature.class)
				.setParameter("owner", captured.getCharacterId())
				.getSingleResult();
			assertEquals("GIFT", gift.getOriginKind());
			assertTrue(gift.isBound());
			assertNull(gift.getCaptorCharacterId());
			assertEquals(gift.getCharacterId(), gift.getOriginCharacterId());
		});
		var quote = names.preview(owner, id);
		var request = new CreatureNamingRequest(UUID.randomUUID().toString(), "护岩", quote.name(), quote.renamed(),
				quote.price());
		naming.rename(owner, id, request);
		naming.rename(owner, id, request);
		transactions.run(() -> {
			var pet = em.find(OwnedCreature.class, id);
			assertTrue(pet.isCustomName());
			assertEquals(1, pet.getRenameCount());
			assertEquals("H", pet.getAbilitySlot());
			assertEquals(pet.getOriginCharacterId(), pet.getCaptorCharacterId());
			assertFalse(pet.isBound());
		});
	}

	@Test
	void failedOuterTransactionDoesNotLeaveCaptureOrItsOrigin() {
		long owner = player();
		long[] id = { 0 };
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			id[0] = creatures.capture(owner, "ROLLBACK_ORIGIN", "STONE_GOLEM", 1, "A", "坚岩", List.of("碎岩击"));
			em.flush();
			throw new IllegalStateException("模拟捕获外层结算失败");
		}));
		transactions.run(() -> assertNull(em.find(OwnedCreature.class, id[0])));
	}

	@Test
	void ownershipChangeCannotEraseSourceOrGrantBuyerPersonalCaptureCredit() {
		long seller = player();
		long buyer = player();
		long id = transactions
			.call(() -> creatures.capture(seller, "ORIGIN_TRANSFER", "STONE_GOLEM", 1, "A", "坚岩", List.of("碎岩击")));
		// 这里只隔离验证归属变化后的来源语义，不把直接改归属夹具当作市场成交验收。
		transactions.run(() -> {
			var pet = em.find(OwnedCreature.class, id);
			pet.setCharacterId(characters.lock(buyer).id());
			pet.setPartyPosition(100);
		});
		assertTrue(queries.hasSourcePrefix(seller, "ORIGIN_TRANSFER"));
		assertFalse(queries.hasSourcePrefix(buyer, "ORIGIN_TRANSFER"));
		assertFalse(queries.hasPlacement(buyer, "ORIGIN_TRANSFER", 100));
		long repeated = transactions
			.call(() -> creatures.capture(seller, "ORIGIN_TRANSFER", "STONE_GOLEM", 1, "A", "坚岩", List.of("碎岩击")));
		assertEquals(id, repeated);
		assertTrue(queries.list(seller).isEmpty());
		assertEquals(1, queries.list(buyer).size());
		var originalDiscovery = queries.encyclopedia(seller)
			.stream()
			.filter(p -> p.species().equals("STONE_GOLEM"))
			.findFirst()
			.orElseThrow();
		assertTrue(originalDiscovery.collected());
		assertEquals(0, originalDiscovery.owned());
		assertTrue(originalDiscovery.rewarded());
		assertEquals(1, originalDiscovery.abilities().size());
	}

}
