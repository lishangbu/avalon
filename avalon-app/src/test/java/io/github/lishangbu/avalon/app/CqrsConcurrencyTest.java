package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.creature.application.query.CreatureQueryService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.world.application.query.QuestQueryService;

import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 CQRS 带来的实际行为，而不只检查类名或目录。 一个真实事务持有角色悲观锁且尚未提交，另一个线程应仍可读到已提交状态。 若读服务误用写侧
 * lock，此测试会在释放锁前超时；finally 保证失败也能释放锁。
 */
class CqrsConcurrencyTest extends SpringIntegrationTest {

	@Autowired
	RegistrationService registration;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService commands;

	@Autowired
	CharacterQueryService characters;

	@Autowired
	InventoryQueryService inventory;

	@Autowired
	CreatureQueryService creatures;

	@Autowired
	QuestQueryService quests;

	/**
	 * 持锁写事务尚未提交期间，独立查询必须在释放锁前完成并只看到旧经验。finally释放闩锁防止失败悬挂，提交后再次读取验证可见性。
	 */
	@Test
	void queriesDoNotWaitForCharacterWriteLock() throws Exception {
		String name = "cqrs_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
		registration.register(name, "Cqrs-test-password-123", "test");
		long owner = accounts.accountId(name);
		commands.create(owner, name, "WARRIOR");
		CountDownLatch locked = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		try (var workers = Executors.newVirtualThreadPerTaskExecutor()) {
			var writer = workers.submit(() -> transactions.run(() -> {
				commands.lock(owner);
				commands.grantExperience(owner, 100);
				locked.countDown();
				try {
					if (!release.await(20, TimeUnit.SECONDS))
						throw new IllegalStateException("测试释放锁超时");
				}
				catch (InterruptedException error) {
					Thread.currentThread().interrupt();
					throw new IllegalStateException(error);
				}
			}));
			try {
				assertTrue(locked.await(5, TimeUnit.SECONDS));
				var reader = workers.submit(() -> {
					assertEquals(0, characters.find(owner).experience(), "未提交经验不可被读到");
					assertTrue(inventory.list(owner).isEmpty());
					assertTrue(creatures.list(owner).isEmpty());
					assertFalse(quests.state(owner).available().isEmpty());
				});
				reader.get(5, TimeUnit.SECONDS);
			}
			finally {
				release.countDown();
			}
			writer.get(5, TimeUnit.SECONDS);
			assertEquals(100, characters.find(owner).experience(), "提交后下一次查询应看到奖励");
		}
	}

}
