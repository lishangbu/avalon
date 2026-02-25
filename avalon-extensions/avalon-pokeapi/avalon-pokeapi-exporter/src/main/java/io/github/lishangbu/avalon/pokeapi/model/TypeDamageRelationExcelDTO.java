package io.github.lishangbu.avalon.pokeapi.model;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/// 属性相互克制关系 Excel数据传输对象
///
/// @author lishangbu
/// @since 2026/2/4
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TypeDamageRelationExcelDTO {
    /// 攻击方 ID
    @ExcelProperty("attacking_type_id")
    private Integer attackingTypeId;

    /// 防御方 ID
    @ExcelProperty("defending_type_id")
    private Integer defendingTypeId;

    /// 伤害倍数
    @ExcelProperty("multiplier")
    private Float multiplier;
}
