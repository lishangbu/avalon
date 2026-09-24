package io.github.lishangbu.avalon.app;

import java.util.Map;
import java.util.UUID;

import tools.jackson.databind.ObjectMapper;
import io.github.lishangbu.avalon.identity.persistence.entity.Account;
import io.github.lishangbu.avalon.identity.persistence.repository.AccountRepository;
import io.github.lishangbu.avalon.market.persistence.mapper.MarketReceiptCodec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.core.JacksonException;

import static org.junit.jupiter.api.Assertions.*;

/** 使用应用实际 ObjectMapper 和数据库，验证回执比较及解析失败后的事务回滚。 */
class CodecPersistenceTest extends SpringIntegrationTest {

	@Autowired
	MarketReceiptCodec receipts;

	@Autowired
	ObjectMapper battles;

	@Autowired
	AccountRepository accounts;

	@Test
	void receiptComparisonIgnoresFieldOrderAndIntegerWrapperType() {
		assertTrue(receipts.matches("{\"quantity\":2,\"price\":10}", Map.of("price", 10L, "quantity", 2L)));
		assertFalse(receipts.matches("{\"quantity\":2}", Map.of("quantity", 3)));
		assertThrows(JacksonException.class, () -> receipts.matches("{broken", Map.of()));
	}

	@Test
	void corruptArchiveRollsBackPreviouslyFlushedChanges() {
		var account = account();
		assertThrows(JacksonException.class, () -> transactions.run(() -> {
			accounts.saveAndFlush(account);
			battles.readValue("{broken", Map.class);
		}));
		assertNotNull(account.getId());
		assertFalse(accounts.existsById(account.getId()));
	}

	@Test
	void encodingFailureRollsBackPreviouslyFlushedChanges() {
		var account = account();
		assertThrows(JacksonException.class, () -> transactions.run(() -> {
			accounts.saveAndFlush(account);
			receipts.encode(new BrokenValue());
		}));
		assertFalse(accounts.existsById(account.getId()));
	}

	private Account account() {
		var account = new Account();
		account.setUsername("codec-" + UUID.randomUUID().toString().substring(0, 12));
		account.setPasswordHash("test-only-hash");
		account.setRoles("PLAYER");
		return account;
	}

	static class BrokenValue {

		public String getValue() {
			throw new IllegalStateException("模拟编码失败");
		}

	}

}
