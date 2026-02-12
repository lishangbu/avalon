package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.*;

import java.io.Serial;
import java.io.Serializable;

import lombok.Data;

/// 进化链环节(ChainLink)实体类
///
/// 表示进化链中的一个环节，包括宝可梦种类和进化细节
///
/// @author lishangbu
/// @since 2026/2/12
@Data
@Entity
@Table(comment = "进化链环节")
public class ChainLink implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;

  /// 主键
  @Id
  @Flex
  @Column(comment = "主键")
  private Long id;

  /// 进化链 ID
  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "evolution_chain_id",
    foreignKey = @ForeignKey(name = "fk_chain_link_evolution_chain"),
    comment = "进化链ID"
  )
  private EvolutionChain evolutionChain;

  /// 宝可梦种类 ID
  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "pokemon_species_id",
    foreignKey = @ForeignKey(name = "fk_chain_link_pokemon_species"),
    comment = "宝可梦种类ID"
  )
  private PokemonSpecies speciesId;

  /// 是否为幼年形态
  @Column(comment = "是否为幼年形态")
  private Boolean isBaby;

}
