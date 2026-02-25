package io.github.lishangbu.avalon.pokeapi.model.pokemon;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import io.github.lishangbu.avalon.pokeapi.model.game.VersionGroup;
import java.util.List;

/// 宝可梦形态模型
///
/// 表示宝可梦的视觉形态（例如外观差异），适用于纯粹的外观变化
///
/// @param id           资源标识符
/// @param name         资源名称
/// @param order        形态排序顺序
/// @param formOrder    同一宝可梦种类内的形态排序顺序
/// @param isDefault    是否为默认形态
/// @param isBattleOnly 是否仅在战斗中出现
/// @param isMega       是否为超级进化形态
/// @param formName     形态名称
/// @param pokemon      此形态所属的宝可梦引用
/// @param types        形态拥有的属性类型列表
/// @param sprites      形态的精灵图像集合
/// @param versionGroup 引入此形态的版本组引用
/// @param names        形态的完整名称（多语言）
/// @param formNames    形态专有的形态名称（多语言）
/// @author lishangbu
/// @see Pokemon
/// @see PokemonFormType
/// @see PokemonFormSprites
/// @see VersionGroup
/// @see Name
/// @since 2025/6/8
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
