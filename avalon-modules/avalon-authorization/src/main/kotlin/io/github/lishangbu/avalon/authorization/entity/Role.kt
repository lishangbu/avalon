package io.github.lishangbu.avalon.authorization.entity

import io.github.lishangbu.avalon.jimmer.id.SnowflakeIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.*

@Entity
interface Role {
    /** ID */
    @Id
    @GeneratedValue(generatorType = SnowflakeIdGenerator::class)
    @JsonConverter(LongToStringConverter::class)
    val id: Long

    /** 状态码 */
    val code: String?

    /** 名称 */
    val name: String?

    /** 启用状态 */
    val enabled: Boolean?

    /** 菜单列表 */
    @ManyToMany
    @JoinTable(
        name = "role_menu_relation",
        joinColumnName = "role_id",
        inverseJoinColumnName = "menu_id",
    )
    val menus: List<Menu>
}
