package io.github.lishangbu.match.trainer

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table
import org.babyfish.jimmer.sql.Version

/** 管理端维护的 Trainer 敏感名称规则记录。 */
@Entity
@Table(name = "match_sensitive_name_rule")
interface MatchSensitiveNameRule {
	@Id
	val id: Long
	/** 管理员录入并用于审计展示的原始词条。 */
	val term: String
	/** 按 Trainer 名称规则规范化并移除分隔符后的匹配键。 */
	val normalizedTerm: String
	/** 规则采用完整匹配或子串匹配。 */
	val matchType: SensitiveNameMatchType
	/** 只有启用的规则参与新 Trainer 名称审核。 */
	val enabled: Boolean
	/** 管理端条件写入使用的乐观并发版本。 */
	@Version
	val revision: Long
}
