package io.github.lishangbu.match.game

import org.babyfish.jimmer.sql.*
import java.time.Instant

/** Match View 的增量公开账本；终态和 Runtime 丢失后仍可重建各查看方已知的信息。 */
@Entity
@Table(name = "match_disclosure_ledger")
interface MatchDisclosureLedger {
	@Id val id: MatchDisclosureLedgerId
	val schemaVersion: Int
	@Serialized val disclosures: MatchDisclosure
	val updatedAt: Instant
}
