package io.github.lishangbu.avalon.dataset.entity;

import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import lombok.Data;

/// 宝可梦特性(PokemonAbility)实体类
///
/// 表示宝可梦与特性的关联关系，包括是否隐藏和槽位信息
///
/// @author lishangbu
/// @since 2025/08/20
@Data
@Entity
@Table(comment = "宝可梦特性")
public class PokemonAbility implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    /// 复合主键
    @EmbeddedId private PokemonAbilityId id;

    /// 是否隐藏特性
    @Column(comment = "是否隐藏特性", nullable = false)
    private Boolean isHidden;

    /// 特性槽位（1-第一槽位，2-第二槽位，3-第三槽位）
    @Column(comment = "特性槽位（1-第一槽位，2-第二槽位，3-第三槽位）", nullable = false)
    private Integer slot;

    /// 宝可梦特性复合主键
    ///
    /// @author lishangbu
    /// @since 2025/08/20
    @Data
    @Embeddable
    public static class PokemonAbilityId implements Serializable {
        @Serial private static final long serialVersionUID = 1L;

        /// 宝可梦引用
        @ManyToOne
        @JoinColumn(name = "pokemon_id", comment = "宝可梦", nullable = false)
        private Pokemon pokemon;

        /// 特性引用
        @ManyToOne
        @JoinColumn(name = "ability_id", nullable = false, comment = "特性")
        private Ability ability;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PokemonAbilityId that = (PokemonAbilityId) o;
            return Objects.equals(getPokemonId(), that.getPokemonId())
                    && Objects.equals(getAbilityId(), that.getAbilityId());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getPokemonId(), getAbilityId());
        }

        private Long getPokemonId() {
            return pokemon != null ? pokemon.getId() : null;
        }

        private Long getAbilityId() {
            return ability != null ? ability.getId() : null;
        }
    }
}
