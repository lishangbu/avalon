package io.github.lishangbu.avalon.market.domain;

import io.github.lishangbu.avalon.platform.BusinessException;
import java.time.Duration;
import java.time.Instant;

/**
 * 市场确定性规则供报价和成交共用。这里只计算，不获取角色锁、不修改金币或生成随机结果。 单价与总额使用精确整数；税按每次实际成交向上取整，不以整单平均税代替部分成交税。
 */
public final class MarketRules {

	public static final int MAX_LISTINGS = 20;

	private static final Duration LIFETIME = Duration.ofHours(48);

	private MarketRules() {
	}

	public static MarketRulesProceeds price(long unitPrice, int quantity) {
		if (unitPrice < 2 || quantity <= 0)
			throw new BusinessException(400, "单价至少为2金币，数量必须为正整数");
		final long gross;
		try {
			gross = Math.multiplyExact(unitPrice, quantity);
		}
		catch (ArithmeticException error) {
			throw new BusinessException(400, "成交总额超出支持范围");
		}
		// ceil(gross / 20) 不使用 gross + 19，避免接近 Long 上限时溢出。
		long tax = gross / 20 + (gross % 20 == 0 ? 0 : 1);
		return new MarketRulesProceeds(gross, tax, gross - tax);
	}

	public static Instant expires(Instant created) {
		return created.plus(LIFETIME);
	}

	/** 到期瞬间已经不可购买，不能依赖后台扫描是否恰好及时执行。 */
	public static void requirePurchase(long seller, long buyer, String state, Instant expires, Instant now,
			int remaining, int quantity) {
		if (seller == buyer)
			throw new BusinessException(409, "不能购买自己的挂单");
		if (!"ACTIVE".equals(state) || !now.isBefore(expires))
			throw new BusinessException(409, "挂单已结束");
		if (quantity <= 0 || quantity > remaining)
			throw new BusinessException(409, "可购买数量不足，请重新确认");
	}

}
