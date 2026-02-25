package io.github.lishangbu.avalon.pokeapi.component;

import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.resource.NamedAPIResourceList;

/// Poke API 请求模板
///
/// 提供对 PokeAPI 的常用访问方法，包括列表查询、按 URI 获取实体以及批量导入并转换插入的辅助方法
///
/// @author lishangbu
/// @since 2025/5/20
public interface PokeApiService {
    /// 获取命名资源列表
    ///
    /// @param typeEnum 资源类型枚举
    /// @return 命名资源列表
    NamedAPIResourceList listNamedAPIResources(PokeDataTypeEnum typeEnum);

    /// 通过指定的 URI 和参数获取指定类型的数据实体
    ///
    /// @param typeEnum 资源类型枚举
    /// @param id       资源对应的唯一 ID
    /// @return 指定类型的数据实体
    <T> T getEntityFromUri(PokeDataTypeEnum typeEnum, Integer id);
}
