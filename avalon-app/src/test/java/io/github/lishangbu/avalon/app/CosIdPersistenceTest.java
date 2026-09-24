package io.github.lishangbu.avalon.app;

import java.util.UUID;

import io.github.lishangbu.avalon.identity.persistence.entity.Account;
import io.github.lishangbu.avalon.identity.persistence.repository.AccountRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

/** 真实数据库验证 Hibernate 发号时机、受管实例与回滚，不使用预分配编号替代生成器。 */
class CosIdPersistenceTest extends SpringIntegrationTest {

	@Autowired
	AccountRepository accounts;

	@Autowired
	EntityManager entityManager;

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
