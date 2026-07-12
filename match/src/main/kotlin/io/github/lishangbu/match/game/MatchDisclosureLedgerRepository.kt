package io.github.lishangbu.match.game

import org.babyfish.jimmer.spring.repository.KRepository

/** 已公开对战事实的 Jimmer 读写边界。 */
interface MatchDisclosureLedgerRepository : KRepository<MatchDisclosureLedger, MatchDisclosureLedgerId>
