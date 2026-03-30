package io.github.lishangbu.avalon.authorization.entity

import io.github.lishangbu.avalon.jimmer.id.SnowflakeIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.*

@Entity
interface Menu {
    /** ID */
    @Id
    @GeneratedValue(generatorType = SnowflakeIdGenerator::class)
    @JsonConverter(LongToStringConverter::class)
    val id: Long

    /** 父节点 */
    @ManyToOne
    val parent: Menu?

    /** 禁用状态 */
    val disabled: Boolean?

    /** 扩展信息 */
    @Serialized
    val extra: String?

    /** 图标 */
    val icon: String?

    /** 密钥 */
    val key: String?

    /** 标签 */
    val label: String?

    /** 显示 */
    val show: Boolean?

    /** 路径 */
    val path: String?

    /** 名称 */
    val name: String?

    /** 重定向 */
    val redirect: String?

    /** 组件 */
    val component: String?

    /** 排序顺序 */
    val sortingOrder: Int?

    /** 固定 */
    val pinned: Boolean?

    /** 显示标签页 */
    val showTab: Boolean?

    /** 启用多标签页 */
    val enableMultiTab: Boolean?

    /** 子节点列表 */
    @OneToMany(
        mappedBy = "parent",
        orderedProps = [OrderedProp("sortingOrder"), OrderedProp("id")],
    )
    val children: List<Menu>
}
