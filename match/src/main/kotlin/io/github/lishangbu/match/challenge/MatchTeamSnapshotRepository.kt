package io.github.lishangbu.match.challenge

import org.babyfish.jimmer.spring.repository.KRepository

/** 不可变 Team Snapshot 的 Jimmer Repository。 */
interface MatchTeamSnapshotRepository : KRepository<MatchTeamSnapshot, Long>
