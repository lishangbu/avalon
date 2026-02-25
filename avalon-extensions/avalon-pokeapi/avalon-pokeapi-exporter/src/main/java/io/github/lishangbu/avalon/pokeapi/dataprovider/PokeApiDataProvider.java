package io.github.lishangbu.avalon.pokeapi.dataprovider;

import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import java.util.List;

/// PokeAPI 数据提供接口
///
/// 统一定义从来源加载指定类型数据的契约，供导出流程复用
///
/// @param <T> 数据实体类型
public interface PokeApiDataProvider<T> {
    /// 按 PokeAPI 类型枚举获取数据列表
    ///
    /// @param typeEnum 数据类型枚举
    /// @param type     目标类型 Class
    /// @return 数据列表
    List<T> fetch(PokeDataTypeEnum typeEnum, Class<T> type);
}
