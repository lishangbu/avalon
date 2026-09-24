package io.github.lishangbu.avalon.market.application.query;

import io.github.lishangbu.avalon.market.persistence.repository.MarketListingRepository;
import org.springframework.stereotype.Component;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import java.time.Instant;
import java.util.List;

/** 到期扫描只读取主键，不在查询事务中持有卖方锁或进行实物交付。 */
@Component
@RequiredArgsConstructor
public class MarketExpiryQueryService {

	private final MarketListingRepository listings;

	@Transactional
	public List<Long> due(Instant now) {
		return listings.expired(now, 100);
	}

}
