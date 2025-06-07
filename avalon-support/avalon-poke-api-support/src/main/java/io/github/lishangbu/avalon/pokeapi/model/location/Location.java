package io.github.lishangbu.avalon.pokeapi.model.location;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.lishangbu.avalon.pokeapi.model.common.GenerationGameIndex;
import io.github.lishangbu.avalon.pokeapi.model.common.Name;
import io.github.lishangbu.avalon.pokeapi.model.common.NamedApiResource;
import java.util.List;

/**
 * 可以在游戏中访问的位置。位置构成了区域的相当大的部分，如城市或路线。
 *
 * @param id 资源的标识符
 * @param name 资源的名称
 * @param region 可以找到此位置的区域{@link Region}
 * @param names 此资源在不同语言中列出的名称{@link Name}
 * @param gameIndices 按代际列出的与此位置相关的游戏索引{@link GenerationGameIndex}列表
 * @param areas 可以在此位置找到的区域{@link LocationArea}
 * @see Region
 * @see Name
 * @see GenerationGameIndex
 * @see LocationArea
 * @author lishangbu
 * @since 2025/5/26
 */
public record Location(
    Integer id,
    String name,
    NamedApiResource<Region> region,
    List<Name> names,
    @JsonProperty("game_indices") List<GenerationGameIndex> gameIndices,
    List<NamedApiResource<LocationArea>> areas) {}
