package io.github.lishangbu.avalon.pokeapi.model;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/// 宝可梦形态 Excel数据传输对象
///
/// @author lishangbu
/// @since 2026/2/10
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PokemonFormExcelDTO {
  /// 唯一标识
  @ExcelProperty("id")
  private Integer id;

  /// 内部名称
  @ExcelProperty("internal_name")
  private String internalName;

  /// 显示名称
  @ExcelProperty("name")
  private String name;

  /// 形态名称
  @ExcelProperty("form_name")
  private String formName;

  /// 是否为默认形态
  @ExcelProperty("is_default")
  private Boolean isDefault;

  /// 是否仅在战斗中出现
  @ExcelProperty("is_battle_only")
  private Boolean isBattleOnly;

  /// 是否为超级进化形态
  @ExcelProperty("is_mega")
  private Boolean isMega;

  /// 宝可梦 ID
  @ExcelProperty("pokemon_id")
  private Integer pokemonId;

  /// 版本组 ID
  @ExcelProperty("version_group_id")
  private Integer versionGroupId;
}
