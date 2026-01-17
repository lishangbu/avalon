package io.github.lishangbu.avalon.admin.mapstruct;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import io.github.lishangbu.avalon.dataset.entity.BerryFlavor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/// MapStruct 映射器：PokeAPI 的 BerryFlavor -> Dataset BerryFlavor
///
/// 按照服务层的 importBerryFlavors 转换规则：
/// - id -> Long
/// - internalName <- name
/// - name 使用本地化名称（从 names 列表中解析）
///
/// @author lishangbu
/// @since 2025/12/24
@Mapper(componentModel = SPRING)
public abstract class BerryFlavorConversionMapper extends AbstractMapstruct {

  @Mapping(target = "id", expression = "java(pokeApiBerryFlavor.id().longValue())")
  @Mapping(target = "internalName", source = "name")
  @Mapping(
      target = "name",
      expression =
          "java(resolveLocalizedNameFromNames(pokeApiBerryFlavor.names(),"
              + " pokeApiBerryFlavor.name()))")
  public abstract BerryFlavor toDatasetBerryFlavor(
      io.github.lishangbu.avalon.pokeapi.model.berry.BerryFlavor pokeApiBerryFlavor);
}
