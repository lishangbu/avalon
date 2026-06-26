package io.github.lishangbu.scheduler.repository

import io.github.lishangbu.scheduler.entity.ScheduledTaskExecutionRecord
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 定时任务执行记录的 Jimmer Repository。
 */
interface ScheduledTaskExecutionRecordRepository : KRepository<ScheduledTaskExecutionRecord, Long>
