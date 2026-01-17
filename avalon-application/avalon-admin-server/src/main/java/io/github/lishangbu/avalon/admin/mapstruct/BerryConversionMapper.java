package io.github.lishangbu.avalon.admin.mapstruct;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import io.github.lishangbu.avalon.dataset.entity.Berry;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/// PokeAPI Berry 到 Dataset Berry 的转换映射器
///
/// 使用 MapStruct 自动生成类型转换代码，简化手动映射逻辑，处理 PokeAPI 数据模型到数据库实体模型的转换
///
/// @author lishangbu
/// @since 2025/12/23
@Mapper(componentModel = SPRING)
public abstract class BerryConversionMapper extends AbstractMapstruct {

  /// 将 PokeAPI Berry 转换为 Dataset Berry
  ///
  /// 执行基本的字段映射，并处理特殊逻辑：
  ///
  /// - ID 转换为 Long 类型
  /// - 从 Item 中获取本地化名称
  /// - 提取 firmness 和 naturalGiftType 的名称
  ///
  /// @param pokeApiBerry PokeAPI Berry 数据
  /// @return Dataset Berry 实体
  @Mapping(target = "id", expression = "java(pokeApiBerry.id().longValue())")
  @Mapping(target = "internalName", source = "name")
  @Mapping(
      target = "name",
      expression =
          "java(resolveLocalizedNameFromResource(pokeApiBerry.item(),"
              + " io.github.lishangbu.avalon.pokeapi.enumeration.PokeDataTypeEnum.ITEM,"
              + " pokeApiBerry.name()))")
  @Mapping(target = "bulk", source = "size")
  @Mapping(target = "firmnessInternalName", source = "firmness.name")
  @Mapping(target = "naturalGiftTypeInternalName", source = "naturalGiftType.name")
  public abstract Berry toDatasetBerry(
      io.github.lishangbu.avalon.pokeapi.model.berry.Berry pokeApiBerry);
}
