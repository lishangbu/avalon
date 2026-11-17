package io.github.lishangbu.avalon.app;

import java.util.UUID;
import java.time.Instant;

import io.github.lishangbu.avalon.identity.persistence.entity.Account;
import io.github.lishangbu.avalon.identity.persistence.repository.AccountRepository;
import jakarta.persistence.EntityManager;
import me.ahoo.cosid.IdGenerator;
import me.ahoo.cosid.codec.IdCodec;
import me.ahoo.cosid.runtime.CosIdRuntime;
import me.ahoo.cosid.time.Layout;
import me.ahoo.cosid.time.TimeBasedGenerator;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

/** 真实数据库验证 Hibernate 发号时机、受管实例与回滚，不使用预分配编号替代生成器。 */
class CosIdPersistenceTest extends SpringIntegrationTest {

	@Autowired
	AccountRepository accounts;

	@Autowired
	EntityManager entityManager;

	@Autowired
	CosIdRuntime runtime;

	@Autowired
	IdGenerator generator;

	@Autowired
	MeterRegistry meters;

	@Test
	void hibernateUsesTheRuntimeGeneratorWithLegacyLayoutAndObservableIssuance() {
		assertSame(generator, runtime.generator("default"));
		var snowflake = runtime.get("default", TimeBasedGenerator.class);
		assertEquals(Layout.SNOWFLAKE_MS, snowflake.layout());
		assertEquals(Instant.parse("2019-12-24T16:00:00Z"), snowflake.layout().epoch());
		long before = runtime.generated("default").orElseThrow();
		var account = account();
		transactions.run(() -> accounts.saveAndFlush(account));
		assertTrue(runtime.generated("default").orElseThrow() > before);
		var parts = snowflake.layout().unpack(account.getId());
		assertEquals(1, parts.node());
		assertEquals(account.getId().longValue(), snowflake.layout().pack(parts.time(), parts.node(), parts.seq()));
		var codec = runtime.get("default", IdCodec.class);
		assertEquals(account.getId().toString(), codec.encode(account.getId()));
		assertEquals(account.getId().longValue(), codec.decode(codec.encode(account.getId())));
		assertTrue(
				meters.get("cosid.generator.generated").tag("name", "default").functionCounter().count() >= before + 1);
	}

	@Test
	void persistAssignsIdToTheSameManagedInstanceAndUpdatesKeepIt() {
		var account = account();
		assertNull(account.getId());
		transactions.run(() -> {
			assertSame(account, accounts.save(account), "新增实体必须走 persist，不产生 merge 副本");
			assertNotNull(account.getId(), "save 返回时即可建立外键关联，不必提前 flush");
			assertTrue(account.getId() > 0);
			assertTrue(entityManager.contains(account));
			account.setRoles("PLAYER,TEST");
		});
		long id = account.getId();
		transactions.run(() -> {
			var loaded = accounts.get(id);
			assertEquals("PLAYER,TEST", loaded.getRoles(), "save 后原对象的修改仍须提交");
			loaded.setRoles("PLAYER");
			assertSame(loaded, accounts.save(loaded));
			assertEquals(id, loaded.getId());
		});
		assertEquals("PLAYER", accounts.get(id).getRoles());
		assertEquals(Long.toString(id),
				jsonMapper.readTree(jsonMapper.writeValueAsString(account)).get("id").asString());
	}

	@Test
	void rolledBackIdIsNotReusedAndNoRowSurvives() {
		var rolledBack = account();
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			accounts.saveAndFlush(rolledBack);
			throw new IllegalStateException("模拟后续业务失败");
		}));
		assertNotNull(rolledBack.getId());
		assertFalse(accounts.existsById(rolledBack.getId()));
		var retry = account();
		transactions.run(() -> accounts.save(retry));
		assertNotEquals(rolledBack.getId(), retry.getId(), "事务回滚不回退 CosId 发号状态");
	}

	private Account account() {
		var account = new Account();
		account.setUsername("id-" + UUID.randomUUID().toString().substring(0, 16));
		account.setPasswordHash("test-only-hash");
		account.setRoles("PLAYER");
		return account;
	}

}
