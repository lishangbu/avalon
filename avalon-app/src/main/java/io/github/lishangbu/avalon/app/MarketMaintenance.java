package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.market.application.command.MarketReturnCommandService;
import io.github.lishangbu.avalon.market.application.query.MarketExpiryQueryService;

import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Instant;

/** 内存调度器 周期唤醒，真正截止时间来自持久化挂单；重启不延长期限，也不需要重建内存计时器。 */
@Component
@RequiredArgsConstructor
public class MarketMaintenance {

	private static final Logger LOG = LoggerFactory.getLogger(MarketMaintenance.class);

	private final MarketExpiryQueryService queries;

	private final MarketReturnCommandService commands;

	/** 每张挂单独立事务，单笔失败留待重试，其余到期挂单继续处理。 */
	void expire() {
		for (long listing : queries.due(Instant.now())) {
			try {
				commands.expire(listing);
			}
			catch (RuntimeException error) {
				LOG.error("市场到期处理失败，保留实物等待重试：listing={}", listing, error);
			}
		}
	}

}
