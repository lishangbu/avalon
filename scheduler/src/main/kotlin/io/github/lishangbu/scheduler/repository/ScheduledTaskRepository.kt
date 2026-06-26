package io.github.lishangbu.scheduler.repository

import io.github.lishangbu.scheduler.entity.ScheduledTask
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 管理端定时任务定义的 Jimmer Repository。
 */
interface ScheduledTaskRepository : KRepository<ScheduledTask, Long>
