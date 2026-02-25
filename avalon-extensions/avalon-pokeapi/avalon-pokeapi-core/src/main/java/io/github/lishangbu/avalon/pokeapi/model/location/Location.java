package io.github.lishangbu.avalon.pokeapi.model.location;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.GenerationGameIndex;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/// 游戏中的位置模型
///
/// 表示可以在游戏中访问的位置，属于某个区域（如城市或路线）
///
/// @param id          资源的标识符
/// @param name        资源名称
/// @param region      所属区域引用
/// @param names       不同语言下的名称列表
/// @param gameIndices 与此位置相关的游戏索引（按代）
/// @param areas       此位置下的子区域列表
/// @author lishangbu
/// @see Region
/// @see Name
/// @see GenerationGameIndex
/// @see LocationArea
/// @since 2025/5/26
public record Location(
        Integer id,
        String name,
        NamedApiResource<Region> region,
        List<Name> names,
        @JsonProperty("game_indices") List<GenerationGameIndex> gameIndices,
        List<NamedApiResource<LocationArea>> areas) {}
