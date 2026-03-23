package io.github.lishangbu.avalon.authorization.entity

import com.fasterxml.jackson.annotation.JsonProperty
import io.github.lishangbu.avalon.jimmer.id.SnowflakeIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.*

@Entity
@Table(name = "users")
interface User {
    /** ID */
    @Id
    @GeneratedValue(generatorType = SnowflakeIdGenerator::class)
    @JsonConverter(LongToStringConverter::class)
    val id: Long

    /** 用户名 */
    val username: String?

    /** 手机 */
    val phone: String?

    /** 邮箱 */
    val email: String?

    /** 头像 */
    val avatar: String?

    /** 加密密码 */
    @get:JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    val hashedPassword: String?

    /** 角色列表 */
    @ManyToMany
    @JoinTable(
        name = "user_role_relation",
        joinColumnName = "user_id",
        inverseJoinColumnName = "role_id",
    )
    val roles: List<Role>
}
