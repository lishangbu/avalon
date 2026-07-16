package io.github.lishangbu.gamedata.entity

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table
import java.time.Instant

/**
 * 同版本原子发布的游戏资料、规则文本与媒体引用集合。
 *
 * 对应 `game_content_pack` 表；聚合边界只保存发布元数据，具体内容实体通过 Content Pack 标识归属。
 * 新增记录的主键统一由 CosId 生成。
 */
@Entity
@Table(name = "game_content_pack")
interface GameContentPack {
	/** 由 CosId 生成并以字符串输出的资料包主键。 */
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	/** 资料包在接口、迁移和缓存中使用的稳定代码。 */
	val code: String
	/** 面向中文管理界面展示的资料包名称。 */
	val name: String
	/** 资料包承载的内容种类。 */
	val contentKind: ContentKind
	/** 草稿、已发布或已退役的生命周期状态。 */
	val status: ContentPackStatus
	/** 内容原始来源的名称。 */
	val sourceName: String
	/** 内容适用的许可证名称。 */
	val licenseName: String
	/** 发布时必须展示的署名文本。 */
	val attribution: String?
	/** 用于验证不可变发布内容的摘要。 */
	val checksum: String?
	/** 资料包首次原子发布的时间。 */
	val publishedAt: Instant?
	/** 资料包记录的创建时间。 */
	val createdAt: Instant
}
