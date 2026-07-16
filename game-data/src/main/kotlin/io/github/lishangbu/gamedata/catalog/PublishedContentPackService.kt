package io.github.lishangbu.gamedata.catalog

import io.github.lishangbu.gamedata.entity.ContentPackStatus
import io.github.lishangbu.gamedata.entity.GameContentPack
import io.github.lishangbu.gamedata.entity.id
import io.github.lishangbu.gamedata.entity.status
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 为写入与玩家目录解析唯一 Published Content Pack。 */
@Service
class PublishedContentPackService(private val sqlClient: KSqlClient) {
	@Transactional(readOnly = true)
	fun requireId(): Long = sqlClient.createQuery(GameContentPack::class) {
		where(table.status eq ContentPackStatus.PUBLISHED)
		select(table.id)
	}.execute().singleOrNull() ?: error("Published Content Pack 不存在或不唯一")
}
