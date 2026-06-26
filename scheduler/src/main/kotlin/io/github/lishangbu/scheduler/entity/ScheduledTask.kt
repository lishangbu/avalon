package io.github.lishangbu.scheduler.entity

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.sql.Column
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Key
import org.babyfish.jimmer.sql.Table
import java.time.OffsetDateTime

/**
 * 可被管理端维护的定时任务定义。
 *
 * Quartz 标准表只保存调度引擎状态，本实体保存管理端需要展示、检索和审计的业务任务元数据。
 */
@Entity
@Table(name = "scheduled_task")
interface ScheduledTask {
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	val id: Long

	@Key
	val code: String

	val handlerCode: String
	val name: String
	val description: String?
	val groupName: String
	val scheduleType: String
	val cronExpression: String?
	val intervalSeconds: Long?
	val runAt: OffsetDateTime?
	val timeZone: String

	@Column(name = "payload_json")
	val payloadJson: String

	val enabled: Boolean
	val createdAt: OffsetDateTime
	val updatedAt: OffsetDateTime
}
