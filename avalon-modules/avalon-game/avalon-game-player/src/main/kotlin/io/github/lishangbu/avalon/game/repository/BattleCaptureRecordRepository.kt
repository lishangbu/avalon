package io.github.lishangbu.avalon.game.repository

import io.github.lishangbu.avalon.game.entity.BattleCaptureRecord
import org.babyfish.jimmer.spring.repository.KRepository

interface BattleCaptureRecordRepository : KRepository<BattleCaptureRecord, Long>
