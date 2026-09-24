package io.github.lishangbu.avalon.market.persistence.repository;

import io.github.lishangbu.avalon.market.persistence.entity.*;
import io.github.lishangbu.avalon.platform.persistence.EntityRepository;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;

/** 类型化持久层入口；写锁及修改均加入调用方事务，不自行拆分业务提交。 */
public interface MarketListingRepository
		extends EntityRepository<MarketListing, Long>, JpaSpecificationExecutor<MarketListing> {

	@Query(value = "select sellerAccountId from MarketListing where id = ?1")
	Long seller(long id);

	@Query(value = "select count(m) from MarketListing m where sellerAccountId = ?1 and state = 'ACTIVE'")
	long active(long seller);

	@Query(value = "select (count(m) > 0) from MarketListing m where sellerAccountId = ?1 and state = 'RETURN_PENDING'")
	boolean hasReturns(long seller);

	@Query(value = "select count(m) from MarketListing m where sellerAccountId = ?1 and state = 'ACTIVE' and expiresAt > ?2")
	long unexpired(long seller, Instant now);

	@Query(value = "select count(m) from MarketListing m where sellerAccountId = ?1 and state = 'ACTIVE' and expiresAt <= ?2")
	long overdue(long seller, Instant now);

	@Query(value = "select id from MarketListing where state = 'ACTIVE' and expiresAt <= ?1 order by expiresAt,id")
	List<Long> expiredRows(Instant now, Pageable page);

	default List<Long> expired(Instant now, int limit) {
		if (limit < 1 || limit > 1000)
			throw new IllegalArgumentException("扫描批次大小无效");
		return expiredRows(now, PageRequest.of(0, limit));
	}

}
