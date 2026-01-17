package io.github.lishangbu.avalon.admin.mapstruct;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import io.github.lishangbu.avalon.dataset.entity.Type;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/// PokeAPI Type 到 Dataset Type 的转换映射器
///
/// 使用 MapStruct 自动生成类型转换代码，简化手动映射逻辑
///
/// @author lishangbu
/// @since 2025/12/24
@Mapper(componentModel = SPRING)
public abstract class TypeConversionMapper extends AbstractMapstruct {

  /// 将 PokeAPI Type 转换为 Dataset Type
  ///
  /// @param pokeApiType PokeAPI Type 数据
  /// @return Dataset Type 实体
  @Mapping(target = "id", expression = "java(pokeApiType.id().longValue())")
  @Mapping(target = "internalName", source = "name")
  @Mapping(
      target = "name",
      expression = "java(resolveLocalizedNameFromNames(pokeApiType.names(), pokeApiType.name()))")
  public abstract Type toDatasetType(
      io.github.lishangbu.avalon.pokeapi.model.pokemon.Type pokeApiType);
}
