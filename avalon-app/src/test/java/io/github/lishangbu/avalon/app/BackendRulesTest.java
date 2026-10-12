package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.battle.GrowthRules;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.item.model.ItemCategory;
import io.github.lishangbu.avalon.content.item.model.ItemDefinition;
import io.github.lishangbu.avalon.content.item.model.ItemGear;
import io.github.lishangbu.avalon.content.item.model.ItemItem;
import io.github.lishangbu.avalon.content.item.model.ItemScope;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.creature.application.query.CreatureQueryService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 覆盖本轮规则边界与注册原子性，复用真实 PostgreSQL，不用实现镜像式 Mock。
 */
class BackendRulesTest extends SpringIntegrationTest {

	@Autowired
	GrowthRules growth;

	@Autowired
	WorldCatalog world;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterQueryService characterQueries;

	@Autowired
	CreatureCommandService creatures;

	@Autowired
	CreatureQueryService creatureQueries;

	@Autowired
	RegistrationService registration;

	@Autowired
	AccountQueryService accountQueries;

	@Autowired
	EntityManager em;

	/**
	 * 检查累计经验在门槛前后升级、满级封顶以及9到10级容量增加。使用设计中的实际数值，防止公式重复后发生边界偏移。
	 */
	@Test
	void cumulativeGrowthAndCarryingBoundaries() {
		assertEquals(1, growth.level(139));
		assertEquals(2, growth.level(140));
		assertEquals(3, growth.level(700));
		assertEquals(264919, growth.addExperience(264819, 100));
		assertEquals(23935620, growth.addExperience(23935619, 100));
		assertEquals(15, growth.level(264820));
		assertEquals(5, growth.carryingCapacity(9));
		assertEquals(6, growth.carryingCapacity(10));
		assertEquals(6, growth.carryingCapacity(15));
	}

	/**
	 * 分别验证拼错属性、负职业加成和合并整数溢出被拒绝，并保留合法正例，避免校验实现直接拒绝全部模板却让测试通过。
	 */
	@Test
	void rejectsUnknownNegativeAndOverflowingEquipmentAttributes() {
		assertThrows(IllegalArgumentException.class, () -> validateGear(Map.of("magicAtack", 1), Map.of()));
		assertThrows(IllegalArgumentException.class,
				() -> validateGear(Map.of(), Map.of("MAGE", Map.of("magicAttack", -1))));
		assertThrows(IllegalArgumentException.class,
				() -> validateGear(Map.of(), Map.of("MAGE", Map.of("magicAtack", 1))));
		assertThrows(ArithmeticException.class,
				() -> validateGear(Map.of("magicAttack", Integer.MAX_VALUE), Map.of("MAGE", Map.of("magicAttack", 1))));
		assertDoesNotThrow(() -> validateGear(Map.of("magicAttack", 2), Map.of("MAGE", Map.of("magicAttack", 3))));
	}

	void validateGear(Map<String, Integer> stats, Map<String, Map<String, Integer>> bonus) {
		var gear = new ItemItem("TEST", "测试武器", "测试装备", ItemCategory.EQUIPMENT, null, ItemScope.GENERAL, false, null,
				null, null, null, null, new ItemGear("WEAPON", 1, Map.of(), stats, bonus, "普通", null));
		ItemCatalog.validate(new ItemDefinition(List.of(gear)), world.definition());
	}

	/**
	 * 真实领取填满5位后验证拒绝，第10级开放第6位，第7只仍被拒绝。通过数据库列表确认失败不会保存半个伙伴或消耗可用位置。
	 */
	@Test
	void tenthLevelOpensSixthPositionAndFullFailureRollsBack() {
		String name = "cap_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
		registration.register(name, "Capacity-test-123", "test");
		long owner = accountQueries.accountId(name);
		characters.create(owner, name, "WARRIOR");
		transactions.run(() -> {
			var c = characters.lock(owner);
			for (int i = 0; i < 5; i++)
				creatures.grant(owner, "capacity:" + i, "CLAW_CAT");
		});
		assertThrows(BusinessException.class, () -> transactions.run(() -> {
			var c = characters.lock(owner);
			creatures.grant(owner, "capacity:5", "CLAW_CAT");
		}));
		transactions.run(() -> {
			characters.grantExperience(owner, Math.toIntExact(growth.threshold(10)));
			var c = characters.lock(owner);
			assertEquals(10, c.level());
			creatures.grant(owner, "capacity:5", "CLAW_CAT");
		});
		long id = Long.parseLong(characterQueries.find(owner).id());
		assertEquals(6, creatureQueries.list(owner).size());
		assertTrue(creatureQueries.list(owner).stream().anyMatch(p -> p.partyPosition() == 5));
		assertThrows(BusinessException.class, () -> transactions.run(() -> {
			var c = characters.lock(owner);
			creatures.grant(owner, "capacity:6", "CLAW_CAT");
		}));
		assertEquals(6, creatureQueries.list(owner).size());
	}

	/**
	 * 在设备插入阶段注入故障，确认账号和首次会话为同一事务。清理触发器后同名注册可成功，证明前一次失败没有留下账号。
	 */
	@Test
	void firstSessionFailureRollsBackRegistrationAndCanRetry() {
		String username = "atomic_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
		transactions.run(() -> {
			em.createNativeQuery("CREATE FUNCTION reject_test_session() RETURNS trigger"
					+ " LANGUAGE plpgsql AS $$ BEGIN IF" + " NEW.label='reject-registration-test' THEN RAISE"
					+ " EXCEPTION 'test session failure'; END IF;" + " RETURN NEW; END $$")
				.executeUpdate();
			em.createNativeQuery("CREATE TRIGGER reject_test_session BEFORE INSERT ON"
					+ " device_session FOR EACH ROW EXECUTE FUNCTION" + " reject_test_session()")
				.executeUpdate();
		});
		try {
			assertThrows(RuntimeException.class,
					() -> registration.register(username, "Atomic-test-1234", "reject-registration-test"));
			transactions.run(() -> assertEquals(0L,
					em.createQuery("select count(a) from Account a where" + " username=:name", Long.class)
						.setParameter("name", username)
						.getSingleResult()));
		}
		finally {
			transactions.run(() -> {
				em.createNativeQuery("DROP TRIGGER reject_test_session ON" + " device_session").executeUpdate();
				em.createNativeQuery("DROP FUNCTION reject_test_session()").executeUpdate();
			});
		}
		assertNotNull(registration.register(username, "Atomic-test-1234", "test").accessToken());
	}

	/**
	 * 独立库存与伙伴接口均要求认证；已认证但尚未创角时返回404，而不是空列表掩盖缺失的角色前置。
	 */
	@Test
	void newDomainEndpointsRequireAuthenticationAndOwnCharacter() {
		client.get().uri("/api/inventory").exchange().expectStatus().isEqualTo(401);
		client.get().uri("/api/creatures").exchange().expectStatus().isEqualTo(401);
		String name = "empty_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
		var tokens = registration.register(name, "Domain-test-1234", "test");
		for (String route : List.of("/api/inventory", "/api/creatures"))
			client.get()
				.uri(route)
				.header("Authorization", "Bearer " + tokens.accessToken())
				.exchange()
				.expectStatus()
				.isEqualTo(404);
	}

}
