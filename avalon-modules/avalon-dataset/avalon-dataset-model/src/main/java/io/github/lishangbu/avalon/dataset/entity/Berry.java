package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.data.jdbc.id.AutoLongIdGenerator;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 树果
 *
 * @author lishangbu
 * @since 2025/5/21
 */
@Table
public class Berry implements AutoLongIdGenerator {
  /** ID */
  @Id private Long id;

  /**
   * 树果名称
   *
   * <p>取百科中的中文名数据
   */
  private String name;

  /**
   * 内部名称
   *
   * <p>取百科中的英文名数据
   */
  private String internalName;

  /** 树生长到下一个阶段所需的时间(小时) */
  private Integer growthTime;

  /** 一棵树上最多可生长的该树果数量 */
  private Integer maxHarvest;

  /** 该树果的大小（毫米） */
  private Integer size;

  /** 该树果的光滑度，用于制作宝可方块或宝芬 */
  private Integer smoothness;

  /** 树果生长时使土壤干燥的速度，数值越高土壤干燥越快 */
  private Integer soilDryness;

  /** 树果的坚硬度 */
  private String firmnessInternalName;

  /** 搭配该树果使用“自然之恩”招式时继承的属性类型 */
  private String naturalGiftTypeInternalName;

  /** 搭配该树果使用“自然之恩”招式时的威力 */
  private Integer naturalGiftPower;

  @Override
  public Long getId() {
    return id;
  }

  @Override
  public void setId(Long id) {
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

  public String getFirmnessInternalName() {
    return firmnessInternalName;
  }

  public void setFirmnessInternalName(String firmnessInternalName) {
    this.firmnessInternalName = firmnessInternalName;
  }

  public String getNaturalGiftTypeInternalName() {
    return naturalGiftTypeInternalName;
  }

  public void setNaturalGiftTypeInternalName(String naturalGiftTypeInternalName) {
    this.naturalGiftTypeInternalName = naturalGiftTypeInternalName;
  }

  public Integer getNaturalGiftPower() {
    return naturalGiftPower;
  }

  public void setNaturalGiftPower(Integer naturalGiftPower) {
    this.naturalGiftPower = naturalGiftPower;
  }
}
