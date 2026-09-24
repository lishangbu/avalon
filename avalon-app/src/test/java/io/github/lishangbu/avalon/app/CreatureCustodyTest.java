package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.persistence.repository.CombatVitalsStore;
import io.github.lishangbu.avalon.battle.persistence.repository.CombatVitalsRepository;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.creature.application.command.*;
import io.github.lishangbu.avalon.creature.application.model.CreatureView;
import io.github.lishangbu.avalon.creature.application.query.CreatureNamingQueryService;
import io.github.lishangbu.avalon.creature.application.query.CreatureQueryService;
import io.github.lishangbu.avalon.creature.application.query.SkillLearningQueryService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** 通过真实托管入口验证不可使用、来源防重、角色隔离与外层失败回滚。 */
class CreatureCustodyTest extends SpringIntegrationTest {

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CreatureCommandService creatures;

	@Autowired
	CreatureCustodyService custody;

	@Autowired
	CreatureManagementService management;

	@Autowired
	CreatureQueryService queries;

	@Autowired
	CreatureNamingQueryService naming;

	@Autowired
	CreatureNamingCommandService rename;

	@Autowired
	SkillLearningQueryService learning;

	@Autowired
	SkillLearningCommandService learn;

	@Autowired
	CombatVitalsStore combatVitalsStore;

	@Autowired
	CombatVitalsRepository combatVitalsRepository;

	@Autowired
	EntityManager em;

	private long ready() {
		String name = "cust_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
		registrations.register(name, "Custody-test-1234", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		// 保留一只同级出战伙伴，托管测试不能通过清空全部可用伙伴来绕过恢复路线保护。
		long companion = transactions
			.call(() -> creatures.capture(owner, "WILD:RETAINED", "CLAW_CAT", 1, "A", "完好防护", List.of("水弹")));
		creatures.position(owner, companion, 0);
		return owner;
	}

	private long capture(long owner, String source) {
		return transactions.call(() -> creatures.capture(owner, source, "STONE_GOLEM", 1, "H", "守护", List.of("碎岩击")));
	}

	private CreatureView view(long owner, long id) {
		return queries.list(owner).stream().filter(p -> p.id().equals(Long.toString(id))).findFirst().orElseThrow();
	}

	private void deposit(long owner, long id, CreatureView expected) {
		transactions.run(() -> custody.deposit(owner, id, expected));
	}

	@Test
	void custodyRemovesAvailablePetButPreservesDiscoveryAndRejectsAllManagement() {
		long owner = ready();
		long id = capture(owner, "WILD:CUSTODY");
		creatures.position(owner, id, 4);
		var expected = view(owner, id);
		deposit(owner, id, expected);
		assertTrue(queries.list(owner).stream().noneMatch(p -> p.id().equals(Long.toString(id))));
		assertTrue(queries.encyclopedia(owner)
			.stream()
			.filter(p -> p.species().equals("STONE_GOLEM"))
			.findFirst()
			.orElseThrow()
			.collected());
		assertEquals(id, capture(owner, "WILD:CUSTODY"));
		assertTrue(queries.list(owner).stream().noneMatch(p -> p.id().equals(Long.toString(id))));
		assertFalse(queries.hasPlacement(owner, "WILD:CUSTODY", 4));
		assertThrows(BusinessException.class, () -> creatures.position(owner, id, 4));
		assertThrows(BusinessException.class, () -> management.lock(owner, id, true));
		assertThrows(BusinessException.class, () -> management.release(owner,
				new CreatureManagementService.Release(UUID.randomUUID().toString(), List.of(expected))));
		assertThrows(BusinessException.class, () -> naming.preview(owner, id));
		assertThrows(BusinessException.class, () -> rename.rename(owner, id,
				new CreatureNamingCommandService.Request(UUID.randomUUID().toString(), "新名字", expected.name(), 0, 0)));
		assertThrows(BusinessException.class, () -> learning.preview(owner, id));
		assertThrows(BusinessException.class, () -> learn.learn(owner, id,
				new SkillLearningCommandService.Request(UUID.randomUUID().toString(), "1", expected.skills())));
		assertThrows(BusinessException.class, () -> deposit(owner, id, expected));
		// 上架后释放的位置可以重新放入真实伙伴，但原托管个体不能被来源重试取回。
		long next = capture(owner, "WILD:NEXT");
		creatures.position(owner, next, 4);
		assertEquals(4, view(owner, next).partyPosition());
	}

	@Test
	void depositProtectsPartyStoryLockedForeignAndStalePetsAndRollsBack() {
		long owner = ready();
		long other = ready();
		long id = capture(owner, "WILD:GUARDS");
		creatures.position(owner, id, 1);
		assertThrows(BusinessException.class, () -> deposit(owner, id, view(owner, id)));
		creatures.position(owner, id, 4);
		var expected = view(owner, id);
		assertThrows(BusinessException.class, () -> deposit(other, id, expected));
		management.lock(owner, id, true);
		assertThrows(BusinessException.class, () -> deposit(owner, id, view(owner, id)));
		management.lock(owner, id, false);
		creatures.position(owner, id, 3);
		assertThrows(BusinessException.class, () -> deposit(owner, id, expected));
		creatures.position(owner, id, 4);
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			custody.deposit(owner, id, expected);
			throw new IllegalStateException("模拟挂单持久化失败");
		}));
		assertEquals(expected, view(owner, id));
		long story = capture(owner, "TUTORIAL:STORY");
		creatures.position(owner, story, 4);
		assertThrows(BusinessException.class, () -> deposit(owner, story, view(owner, story)));
		// 更名后位置仍允许托管，必须依靠完整资料比对拒绝旧确认。
		creatures.position(owner, id, 4);
		rename.rename(owner, id,
				new CreatureNamingCommandService.Request(UUID.randomUUID().toString(), "更名伙伴", expected.name(), 0, 0));
		assertThrows(BusinessException.class, () -> deposit(owner, id, expected));
		deposit(owner, id, view(owner, id));
	}

	/** 测试调用也遵守市场的账号升序双角色锁，避免建立与正式入口相反的锁序。 */
	private void receive(long seller, long buyer, long id) {
		characters.lock(Math.min(seller, buyer));
		characters.lock(Math.max(seller, buyer));
		custody.receive(seller, buyer, id);
	}

	@Test
	void receivePreservesDeadPetIdentityResourcesAndOriginAndRollsBackTogether() {
		long seller = ready();
		long buyer = ready();
		long id = capture(seller, "WILD:TRANSFER");
		creatures.position(seller, id, 4);
		rename.rename(seller, id, new CreatureNamingCommandService.Request(UUID.randomUUID().toString(), "珍藏石偶",
				view(seller, id).name(), 0, 0));
		var expected = view(seller, id);
		long original = transactions.call(() -> characters.lock(seller).id());
		long target = transactions.call(() -> characters.lock(buyer).id());
		transactions.run(() -> combatVitalsStore.saveVitals(original, id, 0, 3));
		deposit(seller, id, expected);
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			receive(seller, buyer, id);
			throw new IllegalStateException("模拟成交结算失败");
		}));
		assertTrue(queries.list(buyer).stream().noneMatch(p -> p.id().equals(Long.toString(id))));
		transactions.run(() -> assertEquals(original, combatVitalsRepository.get(id).getCharacterId()));
		transactions.run(() -> receive(seller, buyer, id));
		var received = view(buyer, id);
		assertEquals(100, received.partyPosition());
		assertEquals(expected.name(), received.name());
		assertEquals(expected.skills(), received.skills());
		assertEquals(expected.abilitySlot(), received.abilitySlot());
		assertEquals(expected.experience(), received.experience());
		assertFalse(queries.hasSourcePrefix(buyer, "WILD:TRANSFER"));
		assertEquals(id, capture(seller, "WILD:TRANSFER"));
		assertTrue(queries.list(seller).stream().noneMatch(p -> p.id().equals(Long.toString(id))));
		transactions.run(() -> {
			var stored = combatVitalsRepository.get(id);
			assertEquals(target, stored.getCharacterId());
			assertEquals(0, stored.getHp());
			assertEquals(3, stored.getMp());
			var provenance = (Object[]) em.createNativeQuery(
					"select origin_character_id, captor_character_id, bound, custom_name, rename_count from owned_creature where id=:id")
				.setParameter("id", id)
				.getSingleResult();
			assertEquals(original, ((Number) provenance[0]).longValue());
			assertEquals(original, ((Number) provenance[1]).longValue());
			assertEquals(false, provenance[2]);
			assertEquals(true, provenance[3]);
			assertEquals(1, ((Number) provenance[4]).intValue());
		});
		assertTrue(queries.encyclopedia(buyer)
			.stream()
			.filter(p -> p.species().equals("STONE_GOLEM"))
			.findFirst()
			.orElseThrow()
			.collected());
		assertThrows(BusinessException.class, () -> transactions.run(() -> receive(seller, buyer, id)));
		// 买入不绑定，允许再次托管；同归属退货也不恢复生命魔力。
		deposit(buyer, id, received);
		transactions.run(() -> receive(buyer, buyer, id));
		transactions.run(() -> assertEquals(0, combatVitalsRepository.get(id).getHp()));
	}

	@Test
	void receiveRejectsFullStorageWithoutLosingCustody() {
		long seller = ready();
		long buyer = ready();
		long id = capture(seller, "WILD:CAPACITY");
		creatures.position(seller, id, 4);
		deposit(seller, id, view(seller, id));
		transactions.run(() -> {
			for (int i = 0; i < 30; i++)
				creatures.capture(buyer, "FULL:" + i, "STONE_GOLEM", 2, "A", "坚岩", List.of("碎岩击"));
		});
		assertThrows(BusinessException.class, () -> transactions.run(() -> receive(seller, buyer, id)));
		assertTrue(queries.list(seller).stream().noneMatch(p -> p.id().equals(Long.toString(id))));
		assertEquals(31, queries.list(buyer).size());
		var released = queries.list(buyer).stream().filter(p -> p.partyPosition() >= 100).findFirst().orElseThrow();
		management.release(buyer,
				new CreatureManagementService.Release(UUID.randomUUID().toString(), List.of(released)));
		transactions.run(() -> receive(seller, buyer, id));
		assertEquals(100, view(buyer, id).partyPosition());
	}

}
