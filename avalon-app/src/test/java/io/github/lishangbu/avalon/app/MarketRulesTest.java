package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.market.domain.MarketRules;
import io.github.lishangbu.avalon.platform.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/** 边界测试不依赖随机采样，明确覆盖每次成交取整和最大整数金额。 */
class MarketRulesTest {

	@Test
	void eachPartialFillPaysItsOwnRoundedTaxAndConservesGold() {
		var whole = MarketRules.price(2, 10);
		assertEquals(20, whole.gross());
		assertEquals(1, whole.tax());
		assertEquals(19, whole.sellerNet());
		var part = MarketRules.price(2, 1);
		assertEquals(1, part.tax());
		assertEquals(1, part.sellerNet());
		assertEquals(10, part.tax() * 10, "分次成交的取整税可能高于整单成交");
		var maximum = MarketRules.price(Long.MAX_VALUE, 1);
		assertEquals(Long.MAX_VALUE, Math.addExact(maximum.tax(), maximum.sellerNet()));
		assertEquals(461168601842738791L, maximum.tax());
		assertThrows(BusinessException.class, () -> MarketRules.price(Long.MAX_VALUE, 2));
		assertThrows(BusinessException.class, () -> MarketRules.price(1, 1));
		assertThrows(BusinessException.class, () -> MarketRules.price(2, 0));
	}

	@Test
	void expiryAndQuantityAreEnforcedAtPurchaseTime() {
		var created = Instant.parse("2026-09-19T00:00:00Z");
		var expires = MarketRules.expires(created);
		assertEquals(Instant.parse("2026-09-21T00:00:00Z"), expires);
		assertDoesNotThrow(() -> MarketRules.requirePurchase(1, 2, "ACTIVE", expires, expires.minusNanos(1), 3, 3));
		assertThrows(BusinessException.class,
				() -> MarketRules.requirePurchase(1, 2, "ACTIVE", expires, expires, 3, 1));
		assertThrows(BusinessException.class,
				() -> MarketRules.requirePurchase(1, 1, "ACTIVE", expires, created, 3, 1));
		assertThrows(BusinessException.class,
				() -> MarketRules.requirePurchase(1, 2, "RETURN_PENDING", expires, created, 3, 1));
		assertThrows(BusinessException.class,
				() -> MarketRules.requirePurchase(1, 2, "ACTIVE", expires, created, 3, 4));
	}

}
