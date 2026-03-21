package io.github.lishangbu.avalon.authorization.entity

import com.fasterxml.jackson.annotation.JsonProperty
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
@Table(name = "users")
interface User {
    @Id
    @GeneratedValue(generatorType = SnowflakeIdGenerator::class)
    @JsonConverter(LongToStringConverter::class)
    val id: Long

    val username: String?

    val phone: String?

    val email: String?

    val avatar: String?

    @get:JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    val hashedPassword: String?

    @ManyToMany
    @JoinTable(
        name = "user_role_relation",
        joinColumnName = "user_id",
        inverseJoinColumnName = "role_id",
    )
    val roles: List<Role>
}
