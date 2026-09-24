package io.github.lishangbu.avalon.app;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.function.Supplier;

/** 测试夹具显式使用独立事务；故障注入及并发线程与真实命令遵守相同提交语义。 */
final class TestTransactions {

	private final TransactionTemplate template;

	TestTransactions(PlatformTransactionManager manager) {
		template = new TransactionTemplate(manager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}

	<T> T call(Supplier<T> work) {
		return template.execute(status -> work.get());
	}

	void run(Runnable work) {
		template.executeWithoutResult(status -> work.run());
	}

}
