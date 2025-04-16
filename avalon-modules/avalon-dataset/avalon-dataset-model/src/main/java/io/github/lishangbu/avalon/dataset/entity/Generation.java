package io.github.lishangbu.avalon.dataset.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

/**
 * 世代信息
 *
 * @author lishangbu
 * @since 2025/4/14
 */
@Entity
public class Generation implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  @Id
  @Comment("主键,标明世代的数字")
  private Integer id;

  @ColumnDefault("''")
  @Comment("世代")
  @Column(nullable = false, length = 10)
  private String code;

  @ColumnDefault("''")
  @Comment("世代名称")
  @Column(nullable = false, length = 20)
  private String name;

  /**
   * 一个世代有多个招式
   *
   * @see Move
   * @see Move#getGeneration()
   */
  @OneToMany(mappedBy = "generation")
  private List<Move> moves;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<Move> getMoves() {
    return moves;
  }

  public void setMoves(List<Move> moves) {
    this.moves = moves;
  }
}
