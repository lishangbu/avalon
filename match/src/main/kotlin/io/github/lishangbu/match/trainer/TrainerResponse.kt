package io.github.lishangbu.match.trainer

import java.time.Instant
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/** Account Trainer API 返回的稳定资源视图。 */
@Immutable
interface TrainerResponse {
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	val displayName: String
	val revision: Long
	val archivedAt: Instant?
}

/** 将内部 Trainer 快照收敛为不暴露账户和幂等命令信息的 API 视图。 */
internal fun TrainerRecord.toResponse() = TrainerResponse {
	id = this@toResponse.id
	displayName = this@toResponse.displayName
	revision = this@toResponse.revision
	archivedAt = this@toResponse.archivedAt
	}
