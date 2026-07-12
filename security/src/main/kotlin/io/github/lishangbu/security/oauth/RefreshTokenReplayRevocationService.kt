package io.github.lishangbu.security.oauth

import io.github.lishangbu.security.repository.OAuth2AuthorizationRecordRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/** 重放请求最终会以 invalid_grant 回滚外层事务，因此 family 删除必须在独立事务中提交。 */
@Service
@ConditionalOnProperty(prefix = "backend.security", name = ["enabled"], havingValue = "true", matchIfMissing = true)
open class RefreshTokenReplayRevocationService(
	private val authorizations: OAuth2AuthorizationRecordRepository,
) {
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	open fun revoke(authorizationId: String) = authorizations.deleteById(authorizationId)
}
