package io.github.lishangbu.avalon.authorization.entity

import io.github.lishangbu.avalon.jimmer.id.SnowflakeIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.JoinTable
import org.babyfish.jimmer.sql.ManyToMany
import org.babyfish.jimmer.sql.Table

@Entity
@Table(name = "role")
interface Role {
    @Id
    @GeneratedValue(generatorType = SnowflakeIdGenerator::class)
    @JsonConverter(LongToStringConverter::class)
    val id: Long

    val code: String?

    val name: String?

    val enabled: Boolean?

    @ManyToMany
    @JoinTable(
        name = "role_menu_relation",
        joinColumnName = "role_id",
        inverseJoinColumnName = "menu_id",
    )
    val menus: List<Menu>
}
