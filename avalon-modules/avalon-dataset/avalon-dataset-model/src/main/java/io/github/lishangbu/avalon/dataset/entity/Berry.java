package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/// 树果(Berry)实体类
///
/// 表示树果的业务信息，包括生长时间、大小、硬度等属性
///
/// @author lishangbu
/// @since 2025/08/20
@Data
@Entity
public class Berry implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /// 主键
  @Id
  @Flex
  @Column(comment = "主键")
  private Long id;

  /// 内部名称
  @Column(comment = "内部名称", length = 100)
  private String internalName;

  /// 名称
  @Column(comment = "名称", length = 100)
  private String name;

  /// 生长到下一个阶段所需的时间(小时)
  @Column(comment = "生长到下一个阶段所需的时间(小时)")
  private Integer growthTime;

  /// 最大结果数
  @Column(comment = "最大结果数")
  private Integer maxHarvest;

  /// 树果大小（毫米）
  @Column(comment = "树果大小（毫米）")
  private Integer bulk;

  /// 光滑度
  @Column(comment = "光滑度")
  private Integer smoothness;

  /// 生长时使土壤干燥的速度，数值越高土壤干燥越快
  @Column(comment = "生长时使土壤干燥的速度，数值越高土壤干燥越快")
  private Integer soilDryness;

  /// 树果的坚硬度
  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(
      name = "firmness_id",
      referencedColumnName = "id",
      comment = "树果的坚硬度",
      foreignKey = @ForeignKey(name = "uk_berry_firmness_id"))
  private BerryFirmness berryFirmness;

  /// 搭配该树果使用“自然之恩”招式时继承的属性类型
  @ManyToOne(cascade = CascadeType.ALL)
  @JoinColumn(
      name = "natural_gift_type_id",
      referencedColumnName = "id",
      comment = "搭配该树果使用“自然之恩”招式时继承的属性类型",
      foreignKey = @ForeignKey(name = "uk_berry_natural_gift_type_id"))
  private Type naturalGiftType;

  /// 搭配该树果使用“自然之恩”招式时的威力
  @Column(comment = "搭配该树果使用“自然之恩”招式时的威力")
  private Integer naturalGiftPower;
}
