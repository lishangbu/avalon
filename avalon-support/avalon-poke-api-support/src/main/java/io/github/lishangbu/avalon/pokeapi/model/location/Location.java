package io.github.lishangbu.avalon.pokeapi.model.location;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.GenerationGameIndex;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/**
 * Locations that can be visited within the games. Locations make up sizable portions of regions,
 * like cities or routes.
 *
 * @param id 资源的标识符
 * @param name 资源的名称
 * @param region 可以找到此位置的区域
 * @param names 此资源在不同语言中列出的名称
 * @param gameIndices 按代际列出的与此位置相关的游戏索引列表
 * @param areas 可以在此位置找到的区域
 * @author lishangbu
 * @since 2025/5/26
 */
public record Location(
    int id,
    String name,
    NamedApiResource<Region> region,
    List<Name> names,
    @JsonProperty("game_indices") List<GenerationGameIndex> gameIndices,
    List<NamedApiResource<LocationArea>> areas) {}
