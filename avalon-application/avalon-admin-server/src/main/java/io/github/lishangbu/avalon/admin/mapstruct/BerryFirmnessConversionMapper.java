package io.github.lishangbu.avalon.admin.mapstruct;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import io.github.lishangbu.avalon.dataset.entity.BerryFirmness;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct 映射器：PokeAPI 的 BerryFirmness -> Dataset BerryFirmness
 *
 * <p>转换规则参照 `BerryFlavorConversionMapper`： - id -> Long - internalName <- name - name 使用本地化名称（从
 * names 列表中解析）
 */
@Mapper(componentModel = SPRING)
public abstract class BerryFirmnessConversionMapper extends AbstractMapstruct {

  @Mapping(target = "id", expression = "java(pokeApiBerryFirmness.id().longValue())")
  @Mapping(target = "internalName", source = "name")
  @Mapping(
      target = "name",
      expression =
          "java(resolveLocalizedNameFromNames(pokeApiBerryFirmness.names(),"
              + " pokeApiBerryFirmness.name()))")
  public abstract BerryFirmness toDatasetBerryFirmness(
      io.github.lishangbu.avalon.pokeapi.model.berry.BerryFirmness pokeApiBerryFirmness);
}
