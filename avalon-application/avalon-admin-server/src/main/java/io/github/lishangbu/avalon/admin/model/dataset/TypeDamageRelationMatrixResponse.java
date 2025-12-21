package io.github.lishangbu.avalon.admin.model.dataset;

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
    private Long attackingTypeId;
    private List<Cell> cells = new ArrayList<>();

    public Row(Long attackingTypeId) {
      this.attackingTypeId = attackingTypeId;
    }
  }

  /** 单元格数据 */
  @Data
  public static class Cell {
    private Long defendingTypeId;
    private Float multiplier;

    public Cell(Long defendingTypeId, Float multiplier) {
      this.defendingTypeId = defendingTypeId;
      this.multiplier = multiplier;
    }
  }
}
