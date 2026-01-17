package io.github.lishangbu.avalon.pokeapi.model.resource;

import io.github.lishangbu.avalon.pokeapi.model.common.APIResource;
import java.util.List;

/// PokeAPI 资源列表（分页）模型
///
/// @param count    此 API 可用资源的总数
/// @param next     列表中下一页的 URL
/// @param previous 列表中上一页的 URL
/// @param results  未命名的 API 资源列表
/// @author lishangbu
/// @since 2025/5/20
public record APIResourceList(
    Integer count, String next, String previous, List<APIResource> results) {}
