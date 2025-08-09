package io.github.lishangbu.avalon.dataset.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import java.io.Serial;
import java.io.Serializable;
import org.hibernate.annotations.Comment;

/**
 * 招式学习器
 *
 * @author lishangbu
 * @since 2025/6/26
 */
@Entity
@Comment("招式学习器")
public class Machine implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  /** ID */
  @Id
  @Comment("主键")
  private Integer id;

  @OneToOne private Item item;
  @OneToOne private Move move;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Item getItem() {
    return item;
  }

  public void setItem(Item item) {
    this.item = item;
  }

  public Move getMove() {
    return move;
  }

  public void setMove(Move move) {
    this.move = move;
  }
}
