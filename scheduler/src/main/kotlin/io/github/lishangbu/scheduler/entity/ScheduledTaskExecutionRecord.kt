package io.github.lishangbu.scheduler.entity

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.sql.Column
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table
import java.time.OffsetDateTime

/**
 * 定时任务的一次执行记录。
 *
 * 该记录面向管理端排错，不参与 Quartz 调度决策。
 */
@Entity
@Table(name = "scheduled_task_execution")
interface ScheduledTaskExecutionRecord {
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	val id: Long

	val taskId: Long
	val taskCode: String
	val handlerCode: String
	val scheduledFireTime: OffsetDateTime?
	val actualFireTime: OffsetDateTime
	val finishedAt: OffsetDateTime?
	val status: String
	val durationMs: Long?
	val refireCount: Int

	@Column(name = "payload_snapshot_json")
	val payloadSnapshotJson: String

	val errorMessage: String?
}
