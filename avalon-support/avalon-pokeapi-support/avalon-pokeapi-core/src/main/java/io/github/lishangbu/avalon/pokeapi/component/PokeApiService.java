package io.github.lishangbu.avalon.pokeapi.component;

import io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum;
import io.github.lishangbu.avalon.pokeapi.model.resource.NamedAPIResourceList;
import io.github.lishangbu.avalon.pokeapi.util.NamedApiResourceUtils;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

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

  /// 导入指定类型的数据资源，并进行转换和插入操作，返回插入后的实体列表
  ///
  /// 该方法会按资源列表逐项获取实体、转换并通过提供的插入函数持久化
  ///
  /// @param dataTypeEnum 资源类型枚举
  /// @param convertFunc  数据转换函数
  /// @param insertFunc   数据插入函数
  /// @param sourceClass  源数据类型
  /// @param <S>          源数据类型
  /// @param <T>          目标数据类型
  /// @return 插入后的实体列表
  default <S, T> List<T> importData(
      PokeDataTypeEnum dataTypeEnum,
      Function<S, T> convertFunc,
      Consumer<T> insertFunc,
      Class<S> sourceClass) {
    NamedAPIResourceList resourceList = listNamedAPIResources(dataTypeEnum);
    return resourceList.results().stream()
        .map(
            namedApiResource ->
                getEntityFromUri(dataTypeEnum, NamedApiResourceUtils.getId(namedApiResource)))
        .filter(sourceClass::isInstance)
        .map(sourceClass::cast)
        .map(convertFunc)
        .filter(Objects::nonNull)
        .peek(insertFunc)
        .collect(Collectors.toList());
  }
}
