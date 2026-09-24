package io.github.lishangbu.avalon.inventory.application.command;

import io.github.lishangbu.avalon.inventory.persistence.repository.InventoryReconciliationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/** 数据库迁移完成后、服务就绪前按角色分事务处理旧档；读取背包不承担维护写入。 */
@Component
@Order(0)
@RequiredArgsConstructor
public class InventoryUpgrade implements ApplicationRunner {

	private final TransactionTemplate transactions;

	private final InventoryReconciliationRepository queue;

	private final InventoryReconciliationService service;

	@Override
	public void run(ApplicationArguments arguments) {
		while (true) {
			var owners = transactions.execute(status -> queue.nextOwners());
			if (owners.isEmpty())
				return;
			owners.forEach(service::repair);
		}
	}

}
