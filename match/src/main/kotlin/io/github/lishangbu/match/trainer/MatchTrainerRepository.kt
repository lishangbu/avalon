package io.github.lishangbu.match.trainer

import org.babyfish.jimmer.spring.repository.KRepository

/** Trainer 聚合根的基础持久化仓库；组合条件查询由 TrainerService 使用 KSqlClient 表达。 */
interface MatchTrainerRepository : KRepository<MatchTrainer, Long>
