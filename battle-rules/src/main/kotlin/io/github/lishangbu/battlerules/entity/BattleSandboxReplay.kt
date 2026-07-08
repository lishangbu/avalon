package io.github.lishangbu.battlerules.entity

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table
import java.time.Instant

/**
 * 战斗沙盒复盘记录。
 *
 * 这张表只保存管理端沙盒已经产生的响应快照，不参与规则计算，也不反向驱动战斗引擎。这样做可以让生产环境排障
 * 保留“当时页面看到的完整事实”，同时避免把临时复盘材料拆进战斗规则资料表，污染真正由引擎读取的三范式规则模型。
 */
@Entity
@Table(name = "battle_sandbox_replay")
interface BattleSandboxReplay {
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	val id: Long

	val title: String
	val formatCode: String
	val turnNumber: Int
	val resolved: Boolean
	val resultSummary: String?
	val responseJson: String
	val savedAt: Instant
}
