package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.game.VersionGroup;
import java.util.List;

/**
 * 某些宝可梦可能以多种视觉上不同的形态出现。这些差异纯粹是外观上的。对于在视觉之外有差异的宝可梦种类的变种，使用"宝可梦"实体来表示这种多样性。
 *
 * @param id 该资源的标识符
 * @param name 该资源的名称
 * @param order 所有形态中形态排序的顺序。多个形态可能具有相同的顺序，此时应回退到按名称排序
 * @param formOrder 在一个宝可梦种类的形态中排序的顺序
 * @param isDefault 对于每个宝可梦使用的默认形态，该值为true（每个宝可梦只有一个形态为默认）
 * @param isBattleOnly 此形态是否只能在战斗中出现
 * @param isMega 此形态是否需要超级进化
 * @param formName 此形态的名称
 * @param pokemon 可以采取此形态的宝可梦{@link Pokemon}
 * @param types 显示此宝可梦形态拥有的属性类型的详细列表{@link PokemonFormType}
 * @param sprites 用于在游戏中描绘此宝可梦形态的精灵图像集合{@link PokemonFormSprites}
 * @param versionGroup 引入此宝可梦形态的版本组{@link VersionGroup}
 * @param names 此宝可梦形态的特定完整名称{@link Name}，如果形态没有特定名称则为空
 * @param formNames 此宝可梦形态的特定形态名称{@link Name}，如果形态没有特定名称则为空
 * @author lishangbu
 * @see Pokemon
 * @see PokemonFormType
 * @see PokemonFormSprites
 * @see VersionGroup
 * @see Name
 * @since 2025/6/8
 */
public record PokemonForm(
    Integer id,
    String name,
    Integer order,
    @JsonProperty("form_order") Integer formOrder,
    @JsonProperty("is_default") Boolean isDefault,
    @JsonProperty("is_battle_only") Boolean isBattleOnly,
    @JsonProperty("is_mega") Boolean isMega,
    @JsonProperty("form_name") String formName,
    NamedApiResource<Pokemon> pokemon,
    List<PokemonFormType> types,
    PokemonFormSprites sprites,
    @JsonProperty("version_group") NamedApiResource<VersionGroup> versionGroup,
    List<Name> names,
    @JsonProperty("form_names") List<Name> formNames) {}
