package io.github.lishangbu.avalon.identity.access.infrastructure.authentication

import io.github.lishangbu.avalon.identity.access.domain.authentication.*
import io.github.lishangbu.avalon.identity.access.domain.iam.IdentityAccessConflict
import io.github.lishangbu.avalon.identity.access.domain.iam.UserId
import io.github.lishangbu.avalon.shared.infra.mutiny.awaitSuspending
import io.github.lishangbu.avalon.shared.infra.sql.*
import io.vertx.mutiny.sqlclient.Pool
import io.vertx.mutiny.sqlclient.Row
import io.vertx.mutiny.sqlclient.SqlConnection
import io.vertx.mutiny.sqlclient.Tuple
import jakarta.enterprise.context.ApplicationScoped
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

@ApplicationScoped
/**
 * 认证 SQL 仓储负责把本地认证的关键安全语义落到数据库事务里：
 * - 同一 user + clientType 的并发会话上限
 * - refresh token rotation
 * - token reuse detection
 * - 会话撤销与 refresh token 级联失效
 */
class AuthenticationSqlRepository(
    private val pool: Pool,
) : AuthenticationRepository {
    override suspend fun findUserByIdentity(
        identityType: IdentityType,
        normalizedPrincipal: String,
    ): AuthenticationUser? =
        translateSqlErrors(::mapDatabaseError) {
            pool.preparedQuery(
                """
                SELECT id AS user_id, username, enabled, password_hash
                FROM iam.user_account
                WHERE ${identityColumn(identityType)} = $1
                """.trimIndent(),
            ).execute(Tuple.of(normalizedPrincipal))
                .awaitSuspending()
                .firstOrNull()
                ?.let(::mapAuthenticationUser)
        }

    override suspend fun createSession(
        user: AuthenticationUser,
        request: SessionCreationRequest,
    ): UserSession =
        translateSqlErrors(::mapDatabaseError) {
            pool.withSuspendingTransaction { connection ->
                /*
                 * 登录成功后的写路径必须放在一个本地事务里：
                 * 1. 锁定并计算当前活跃 session
                 * 2. 超限时撤销最旧 session
                 * 3. 创建新 session
                 * 4. 写入首个 ACTIVE refresh token
                 * 5. 更新 user_account.last_login_at
                 *
                 * 这样可以避免“会话创建成功但 refresh token 未落库”之类的半完成状态。
                 */
                val activeSessionIds =
                    connection.preparedQuery(
                        """
                        SELECT id
                        FROM iam.user_session
                        WHERE user_id = $1
                          AND client_type = $2
                          AND status = 'ACTIVE'
                        ORDER BY id ASC
                        """.trimIndent(),
                    ).execute(Tuple.of(user.id.value, request.clientType.name))
                        .awaitSuspending()
                        .toRows()
                        .map { it.getUUID("id") }

                // 并发上限只约束同一 user + clientType 的活跃会话；超限时踢掉最早建立的 session。
                val overflowCount = activeSessionIds.size - request.maxActiveSessions + 1
                if (overflowCount > 0) {
                    revokeSessions(
                        connection,
                        activeSessionIds.take(overflowCount),
                        SessionRevokedReason.SESSION_LIMIT_EVICTED,
                        request.issuedAt
                    )
                }

                val sessionId =
                    connection.preparedQuery(
                        """
                        INSERT INTO iam.user_session (
                            session_key,
                            user_id,
                            client_type,
                            device_name,
                            device_fingerprint,
                            user_agent,
                            ip,
                            status,
                            revoked_reason,
                            last_authenticated_at,
                            last_refreshed_at,
                            expires_at
                        )
                        VALUES ($1, $2, $3, $4, $5, $6, $7, 'ACTIVE', NULL, $8, $9, $10)
                        RETURNING id
                        """.trimIndent(),
                    ).execute(
                        Tuple.tuple()
                            .addString(request.sessionKey)
                            .addValue(user.id.value)
                            .addString(request.clientType.name)
                            .addValue(request.deviceName)
                            .addValue(request.deviceFingerprint)
                            .addValue(request.userAgent)
                            .addValue(request.ip)
                            .addOffsetDateTime(request.issuedAt.atOffset(ZoneOffset.UTC))
                            .addOffsetDateTime(request.issuedAt.atOffset(ZoneOffset.UTC))
                            .addOffsetDateTime(request.refreshTokenExpiresAt.atOffset(ZoneOffset.UTC)),
                    ).awaitSuspending()
                        .first()
                        .getUUID("id")

                connection.preparedQuery(
                    """
                    INSERT INTO iam.refresh_token (
                        session_id,
                        token_hash,
                        status,
                        expires_at,
                        rotated_from_id,
                        used_at,
                        revoked_at
                    )
                    VALUES ($1, $2, 'ACTIVE', $3, NULL, NULL, NULL)
                    """.trimIndent(),
                ).execute(
                    Tuple.tuple()
                        .addValue(sessionId)
                        .addString(request.refreshTokenHash)
                        .addOffsetDateTime(request.refreshTokenExpiresAt.atOffset(ZoneOffset.UTC)),
                ).awaitSuspending()

                connection.preparedQuery(
                    """
                    UPDATE iam.user_account
                    SET last_login_at = $1
                    WHERE id = $2
                    """.trimIndent(),
                ).execute(
                    Tuple.tuple()
                        .addOffsetDateTime(request.issuedAt.atOffset(ZoneOffset.UTC))
                        .addValue(user.id.value),
                ).awaitSuspending()

                requireSession(connection, sessionId)
            }
        }

    override suspend fun rotateRefreshToken(request: RefreshTokenRotationRequest): RefreshTokenRotationResult? =
        translateSqlErrors(::mapDatabaseError) {
            pool.withSuspendingTransaction transaction@{ connection ->
                /*
                 * refresh token rotation 以 token_hash 为入口并使用 FOR UPDATE 锁住当前 token 行。
                 * 这样同一条 refresh token 的并发刷新请求会被串行化处理，避免重复颁发多个新 token。
                 */
                val row =
                    connection.preparedQuery(
                        """
                        SELECT
                            rt.id AS refresh_token_id,
                            rt.status AS refresh_token_status,
                            rt.expires_at AS refresh_token_expires_at,
                            us.id AS session_id,
                            us.session_key,
                            us.user_id,
                            us.client_type,
                            us.device_name,
                            us.device_fingerprint,
                            us.user_agent,
                            us.ip,
                            us.status AS session_status,
                            us.revoked_reason,
                            us.last_authenticated_at,
                            us.last_refreshed_at,
                            us.expires_at AS session_expires_at,
                            us.version,
                            ua.username,
                            ua.enabled,
                            ua.password_hash
                        FROM iam.refresh_token rt
                        JOIN iam.user_session us ON us.id = rt.session_id
                        JOIN iam.user_account ua ON ua.id = us.user_id
                        WHERE rt.token_hash = $1
                        FOR UPDATE
                        """.trimIndent(),
                    ).execute(Tuple.of(request.currentTokenHash))
                        .awaitSuspending()
                        .firstOrNull()
                        ?: return@transaction null

                val sessionId = row.getUUID("session_id")
                val refreshTokenStatus = RefreshTokenStatus.valueOf(row.getString("refresh_token_status"))
                val sessionStatus = SessionStatus.valueOf(row.getString("session_status"))
                val refreshExpiresAt = row.instant("refresh_token_expires_at")
                val userEnabled = row.getBoolean("enabled")

                // 已 rotation 或已撤销的 refresh token 再次被使用时，按 token reuse 处理并撤销整个 session。
                if (refreshTokenStatus != RefreshTokenStatus.ACTIVE) {
                    if (sessionStatus == SessionStatus.ACTIVE) {
                        revokeSessions(
                            connection,
                            listOf(sessionId),
                            SessionRevokedReason.TOKEN_REUSE_DETECTED,
                            request.rotatedAt
                        )
                    }
                    return@transaction null
                }

                if (!userEnabled) {
                    revokeSessions(connection, listOf(sessionId), SessionRevokedReason.USER_DISABLED, request.rotatedAt)
                    return@transaction null
                }

                if (sessionStatus != SessionStatus.ACTIVE || refreshExpiresAt.isBefore(request.rotatedAt)) {
                    expireRefreshToken(connection, row.getUUID("refresh_token_id"), request.rotatedAt)
                    return@transaction null
                }

                connection.preparedQuery(
                    """
                    UPDATE iam.refresh_token
                    SET status = 'ROTATED',
                        used_at = $1
                    WHERE id = $2
                    """.trimIndent(),
                ).execute(
                    Tuple.tuple()
                        .addOffsetDateTime(request.rotatedAt.atOffset(ZoneOffset.UTC))
                        .addValue(row.getUUID("refresh_token_id")),
                ).awaitSuspending()

                connection.preparedQuery(
                    """
                    INSERT INTO iam.refresh_token (
                        session_id,
                        token_hash,
                        status,
                        expires_at,
                        rotated_from_id,
                        used_at,
                        revoked_at
                    )
                    VALUES ($1, $2, 'ACTIVE', $3, $4, NULL, NULL)
                    """.trimIndent(),
                ).execute(
                    Tuple.tuple()
                        .addValue(sessionId)
                        .addString(request.newTokenHash)
                        .addOffsetDateTime(request.newRefreshTokenExpiresAt.atOffset(ZoneOffset.UTC))
                        .addValue(row.getUUID("refresh_token_id")),
                ).awaitSuspending()

                connection.preparedQuery(
                    """
                    UPDATE iam.user_session
                    SET last_refreshed_at = $1,
                        expires_at = $2,
                        version = version + 1
                    WHERE id = $3
                    """.trimIndent(),
                ).execute(
                    Tuple.tuple()
                        .addOffsetDateTime(request.rotatedAt.atOffset(ZoneOffset.UTC))
                        .addOffsetDateTime(request.newRefreshTokenExpiresAt.atOffset(ZoneOffset.UTC))
                        .addValue(sessionId),
                ).awaitSuspending()

                RefreshTokenRotationResult(
                    user = mapAuthenticationUser(row),
                    session =
                        mapUserSession(
                            row = row,
                            lastRefreshedAt = request.rotatedAt,
                            expiresAt = request.newRefreshTokenExpiresAt,
                            version = row.getLong("version") + 1,
                        ),
                )
            }
        }

    override suspend fun revokeSession(
        userId: UserId,
        sessionId: String,
        reason: SessionRevokedReason,
        occurredAt: Instant,
    ): Boolean =
        translateSqlErrors(::mapDatabaseError) {
            pool.withSuspendingTransaction transaction@{ connection ->
                val row =
                    connection.preparedQuery(
                        """
                        SELECT id
                        FROM iam.user_session
                        WHERE user_id = $1
                          AND session_key = $2
                          AND status = 'ACTIVE'
                        FOR UPDATE
                        """.trimIndent(),
                    ).execute(Tuple.of(userId.value, sessionId))
                        .awaitSuspending()
                        .firstOrNull()
                        ?: return@transaction false

                revokeSessions(connection, listOf(row.getUUID("id")), reason, occurredAt)
                true
            }
        }

    override suspend fun revokeAllSessions(
        userId: UserId,
        reason: SessionRevokedReason,
        occurredAt: Instant,
    ): Int =
        translateSqlErrors(::mapDatabaseError) {
            pool.withSuspendingTransaction { connection ->
                val sessionIds =
                    connection.preparedQuery(
                        """
                        SELECT id
                        FROM iam.user_session
                        WHERE user_id = $1
                          AND status = 'ACTIVE'
                        FOR UPDATE
                        """.trimIndent(),
                    ).execute(Tuple.of(userId.value))
                        .awaitSuspending()
                        .toRows()
                        .map { it.getUUID("id") }

                revokeSessions(connection, sessionIds, reason, occurredAt)
                sessionIds.size
            }
        }

    override suspend fun listSessions(userId: UserId): List<UserSession> =
        translateSqlErrors(::mapDatabaseError) {
            pool.preparedQuery(
                """
                SELECT
                    id,
                    session_key,
                    user_id,
                    client_type,
                    device_name,
                    device_fingerprint,
                    user_agent,
                    ip,
                    status,
                    revoked_reason,
                    last_authenticated_at,
                    last_refreshed_at,
                    expires_at,
                    version
                FROM iam.user_session
                WHERE user_id = $1
                  AND status = 'ACTIVE'
                ORDER BY id DESC
                """.trimIndent(),
            ).execute(Tuple.of(userId.value))
                .awaitSuspending()
                .toRows()
                .map(::mapUserSession)
        }

    override suspend fun recordAuthenticationLog(entry: AuthenticationLogEntry) {
        translateSqlErrors(::mapDatabaseError) {
            pool.preparedQuery(
                """
                INSERT INTO iam.authentication_log (
                    client_id,
                    error_message,
                    grant_type,
                    identity_type,
                    ip,
                    occurred_at,
                    principal,
                    result_code,
                    session_id,
                    success,
                    user_agent,
                    user_id,
                    username
                )
                VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13)
                """.trimIndent(),
            ).execute(
                Tuple.tuple()
                    .addValue(entry.clientId)
                    .addValue(entry.errorMessage)
                    .addValue(entry.grantType)
                    .addValue(entry.identityType?.name)
                    .addValue(entry.ip)
                    .addOffsetDateTime(entry.occurredAt.atOffset(ZoneOffset.UTC))
                    .addValue(entry.principal)
                    .addString(entry.resultCode)
                    .addValue(entry.sessionId)
                    .addBoolean(entry.success)
                    .addValue(entry.userAgent)
                    .addValue(entry.userId?.value)
                    .addValue(entry.username),
            ).awaitSuspending()
        }
    }

    private suspend fun revokeSessions(
        connection: SqlConnection,
        sessionIds: List<UUID>,
        reason: SessionRevokedReason,
        occurredAt: Instant,
    ) {
        if (sessionIds.isEmpty()) {
            return
        }

        // session 撤销与 ACTIVE refresh token 撤销保持同事务推进，确保旧会话无法继续刷新。
        for (sessionId in sessionIds) {
            connection.preparedQuery(
                """
                UPDATE iam.user_session
                SET status = 'REVOKED',
                    revoked_reason = $1,
                    version = version + 1
                WHERE id = $2
                  AND status = 'ACTIVE'
                """.trimIndent(),
            ).execute(Tuple.of(reason.name, sessionId))
                .awaitSuspending()

            connection.preparedQuery(
                """
                UPDATE iam.refresh_token
                SET status = 'REVOKED',
                    revoked_at = $1
                WHERE session_id = $2
                  AND status = 'ACTIVE'
                """.trimIndent(),
            ).execute(
                Tuple.tuple()
                    .addOffsetDateTime(occurredAt.atOffset(ZoneOffset.UTC))
                    .addValue(sessionId),
            ).awaitSuspending()
        }
    }

    private suspend fun expireRefreshToken(
        connection: SqlConnection,
        refreshTokenId: UUID,
        occurredAt: Instant,
    ) {
        connection.preparedQuery(
            """
            UPDATE iam.refresh_token
            SET status = 'EXPIRED',
                used_at = $1
            WHERE id = $2
              AND status = 'ACTIVE'
            """.trimIndent(),
        ).execute(
            Tuple.tuple()
                .addOffsetDateTime(occurredAt.atOffset(ZoneOffset.UTC))
                .addValue(refreshTokenId),
        ).awaitSuspending()
    }

    private suspend fun requireSession(
        connection: SqlConnection,
        sessionId: UUID,
    ): UserSession =
        connection.preparedQuery(
            """
            SELECT
                id,
                session_key,
                user_id,
                client_type,
                device_name,
                device_fingerprint,
                user_agent,
                ip,
                status,
                revoked_reason,
                last_authenticated_at,
                last_refreshed_at,
                expires_at,
                version
            FROM iam.user_session
            WHERE id = $1
            """.trimIndent(),
        ).execute(Tuple.of(sessionId))
            .awaitSuspending()
            .first()
            .let(::mapUserSession)

    private fun identityColumn(identityType: IdentityType): String =
        when (identityType) {
            IdentityType.USERNAME -> "username_normalized"
            IdentityType.EMAIL -> "email_normalized"
            IdentityType.PHONE -> "phone_normalized"
        }

    private fun mapDatabaseError(exception: Throwable): Throwable {
        val message = exception.message ?: "Authentication data access failure"
        return when {
            message.contains("duplicate key", ignoreCase = true) ->
                IdentityAccessConflict("The submitted authentication data conflicts with an existing record.")

            message.contains("foreign key", ignoreCase = true) ->
                IdentityAccessConflict("Referenced authentication data does not exist, or the record is still referenced.")

            message.contains("check constraint", ignoreCase = true) ->
                IdentityAccessConflict("The submitted authentication data violates a database validation rule.")

            else -> exception
        }
    }

    private fun mapAuthenticationUser(row: Row): AuthenticationUser =
        AuthenticationUser(
            id = UserId(row.uuidValue("user_id", "id")),
            username = row.getString("username"),
            enabled = row.getBoolean("enabled"),
            passwordHash = row.getString("password_hash"),
        )

    private fun mapUserSession(row: Row): UserSession =
        mapUserSession(
            row = row,
            lastRefreshedAt = row.instant("last_refreshed_at"),
            expiresAt = row.instant("expires_at"),
            version = row.getLong("version"),
        )

    private fun mapUserSession(
        row: Row,
        lastRefreshedAt: Instant,
        expiresAt: Instant,
        version: Long,
    ): UserSession =
        UserSession(
            id = row.uuidValue("session_id", "id"),
            sessionKey = row.getString("session_key"),
            userId = UserId(row.getUUID("user_id")),
            clientType = ClientType.valueOf(row.getString("client_type")),
            deviceName = row.getString("device_name"),
            deviceFingerprint = row.getString("device_fingerprint"),
            userAgent = row.getString("user_agent"),
            ip = row.getString("ip"),
            status = SessionStatus.valueOf(row.stringValue("session_status", "status")),
            revokedReason = row.getString("revoked_reason")?.let(SessionRevokedReason::valueOf),
            lastAuthenticatedAt = row.instant("last_authenticated_at"),
            lastRefreshedAt = lastRefreshedAt,
            expiresAt = expiresAt,
            version = version,
        )

}

