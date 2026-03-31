package io.github.lishangbu.avalon.dataset.entity

import io.github.lishangbu.avalon.jimmer.id.SnowflakeIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.ManyToOne
import org.babyfish.jimmer.sql.OneToMany

@Entity
interface EvolutionChain {
    /** ID */
    @Id
    @GeneratedValue(generatorType = SnowflakeIdGenerator::class)
    @JsonConverter(LongToStringConverter::class)
    val id: Long

    /**
     * 幼年触发道具。
     *
     * 这是链级别元数据；具体某条进化边的条件存储在 `pokemon_evolution` 中。
     */
    @ManyToOne
    val babyTriggerItem: Item?

    /** 进化边列表 */
    @OneToMany(mappedBy = "evolutionChain")
    val pokemonEvolutions: List<PokemonEvolution>
}
