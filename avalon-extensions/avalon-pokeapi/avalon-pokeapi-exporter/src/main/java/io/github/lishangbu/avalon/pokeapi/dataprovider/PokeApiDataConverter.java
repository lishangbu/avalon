package io.github.lishangbu.avalon.pokeapi.dataprovider;

/// PokeAPI 数据转换接口
///
/// 统一定义从来源转换指定类型数据的契约，供导出流程复用
///
/// @author lishangbu
/// @since 2026/2/5
@FunctionalInterface
public interface PokeApiDataConverter<T, R> {
    /// Applies this function to the given argument.
    ///
    /// @param t the function argument
    /// @return the function result
    R convert(T t);
}
