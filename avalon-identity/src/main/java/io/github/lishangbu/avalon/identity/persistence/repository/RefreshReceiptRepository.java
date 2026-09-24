package io.github.lishangbu.avalon.identity.persistence.repository;

import io.github.lishangbu.avalon.identity.persistence.entity.RefreshReceipt;
import io.github.lishangbu.avalon.platform.persistence.EntityRepository;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import org.springframework.data.jpa.repository.*;

/**
 * 刷新凭证属于身份领域，调度模块不直接操作其表或实体。
 */
public interface RefreshReceiptRepository extends EntityRepository<RefreshReceipt, String> {

	@Override
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("from RefreshReceipt where tokenHash = ?1")
	RefreshReceipt lock(String id);

	@Modifying
	@Query("delete from RefreshReceipt e where expiresAt < ?1")
	int deleteExpired(Instant now);

}
