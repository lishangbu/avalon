package io.github.lishangbu.avalon.dataset.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.List;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

/**
 * 树果硬度
 *
 * @author lishangbu
 * @since 2025/5/22
 */
@Comment("树果硬度")
@Entity
public class BerryFirmness {
  /** ID */
  @Id
  @Comment("主键")
  private Integer id;

  /** 树果名称 */
  @Column(nullable = false, length = 100)
  @ColumnDefault("''")
  @Comment("名称")
  private String name;

  /** 内部名称 */
  @Column(nullable = false, length = 100)
  @ColumnDefault("''")
  @Comment("内部名称")
  private String internalName;

  /** 具有该硬度的树果列表 */
  @OneToMany(mappedBy = "firmness")
  private List<Berry> berries;

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

  public List<Berry> getBerries() {
    return berries;
  }

  public void setBerries(List<Berry> berries) {
    this.berries = berries;
  }
}
