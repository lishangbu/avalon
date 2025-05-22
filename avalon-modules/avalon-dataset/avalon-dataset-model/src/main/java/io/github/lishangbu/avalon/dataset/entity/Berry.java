package io.github.lishangbu.avalon.dataset.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

/**
 * 树果
 *
 * @author lishangbu
 * @since 2025/5/21
 */
@Comment("树果")
@Entity
public class Berry {
  /** ID */
  @Id
  @Comment("主键")
  private Integer id;

  /**
   * 树果名称
   *
   * <p>取百科中的中文名数据
   */
  @Column(nullable = false, length = 100)
  @ColumnDefault("''")
  @Comment("名称")
  private String name;

  /**
   * 内部名称
   *
   * <p>取百科中的英文名数据
   */
  @Column(nullable = false, length = 100)
  @ColumnDefault("''")
  @Comment("内部名称")
  private String internalName;

  /** 树生长到下一个阶段所需的时间(小时) */
  @Column(nullable = false)
  @Comment("生长到下一个阶段所需的时间(小时)")
  private Integer growthTime;

  /** 一棵树上最多可生长的该树果数量 */
  @Column(nullable = false)
  @Comment("最大结果数")
  private Integer maxHarvest;

  /** 该树果的大小（毫米） */
  @Column(nullable = false)
  @Comment("大小（毫米）")
  private Integer size;

  /** 该树果的光滑度，用于制作宝可方块或宝芬 */
  @Comment("光滑度")
  private Integer smoothness;

  /** 树果生长时使土壤干燥的速度，数值越高土壤干燥越快 */
  @Comment("生长时使土壤干燥的速度，数值越高土壤干燥越快")
  private Integer soilDryness;

  @ManyToOne
  @JoinColumn(
      name = "firmness_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_berry_firmness_id"))
  private BerryFirmness firmness;

  /** 搭配该树果使用“自然之恩”招式时继承的属性类型 */
  @ManyToOne
  @JoinColumn(
      name = "natural_gift_type_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_berry_natural_gift_type_id"))
  private Type naturalGiftType;

  /** 搭配该树果使用“自然之恩”招式时的威力 */
  private Integer naturalGiftPower;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getInternalName() {
    return internalName;
  }

  public void setInternalName(String internalName) {
    this.internalName = internalName;
  }

  public Integer getGrowthTime() {
    return growthTime;
  }

  public void setGrowthTime(Integer growthTime) {
    this.growthTime = growthTime;
  }

  public Integer getMaxHarvest() {
    return maxHarvest;
  }

  public void setMaxHarvest(Integer maxHarvest) {
    this.maxHarvest = maxHarvest;
  }

  public Integer getSize() {
    return size;
  }

  public void setSize(Integer size) {
    this.size = size;
  }

  public Integer getSmoothness() {
    return smoothness;
  }

  public void setSmoothness(Integer smoothness) {
    this.smoothness = smoothness;
  }

  public Integer getSoilDryness() {
    return soilDryness;
  }

  public void setSoilDryness(Integer soilDryness) {
    this.soilDryness = soilDryness;
  }

  public Type getNaturalGiftType() {
    return naturalGiftType;
  }

  public void setNaturalGiftType(Type naturalGiftType) {
    this.naturalGiftType = naturalGiftType;
  }

  public Integer getNaturalGiftPower() {
    return naturalGiftPower;
  }

  public void setNaturalGiftPower(Integer naturalGiftPower) {
    this.naturalGiftPower = naturalGiftPower;
  }

  public BerryFirmness getFirmness() {
    return firmness;
  }

  public void setFirmness(BerryFirmness firmness) {
    this.firmness = firmness;
  }
}
