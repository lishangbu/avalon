package io.github.lishangbu.avalon.admin.model.dataset;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 属性克制矩阵展示响应
 *
 * <p>为前端表格展示提供按攻击属性分组的行与默认的防御属性列表
 *
 * @author lishangbu
 * @since 2025/12/08
 */
@Data
public class TypeDamageRelationMatrixResponse {
  /** 矩阵行，每行代表一个攻击属性 */
  private final List<Row> rows = new ArrayList<>();

  /** 单行数据 */
  @Data
  public static class Row {
    private Integer attackingTypeId;
    private List<Cell> cells = new ArrayList<>();

    public Row(Integer attackingTypeId) {
      this.attackingTypeId = attackingTypeId;
    }
  }

  /** 单元格数据 */
  @Data
  public static class Cell {
    private Integer defendingTypeId;
    private BigDecimal multiplier;

    public Cell(Integer defendingTypeId, BigDecimal multiplier) {
      this.defendingTypeId = defendingTypeId;
      this.multiplier = multiplier;
    }
  }
}
