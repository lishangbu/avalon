package io.github.lishangbu.avalon.authorization.entity

import io.github.lishangbu.avalon.jimmer.id.SnowflakeIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table

@Entity
@Table(name = "menu")
interface Menu {
    @Id
    @GeneratedValue(generatorType = SnowflakeIdGenerator::class)
    @JsonConverter(LongToStringConverter::class)
    val id: Long

    val parentId: Long?

    val disabled: Boolean?

    val extra: String?

    val icon: String?

    val key: String?

    val label: String?

    val show: Boolean?

    val path: String?

    val name: String?

    val redirect: String?

    val component: String?

    val sortingOrder: Int?

    val pinned: Boolean?

    val showTab: Boolean?

    val enableMultiTab: Boolean?
}
