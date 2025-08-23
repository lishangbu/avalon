package io.github.lishangbu.avalon.dataset.entity;

import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/**
 * 招式(Move)实体类
 *
 * @author lishangbu
 * @since 2025/08/20
 */
@Data
public class Move implements Serializable {
  @Serial private static final long serialVersionUID = -30304372663754761L;

  /** 主键 */
  private Long id;

  /** 内部名称 */
  private String internalName;

  /** 道具口袋名称 */
  private String name;

  /** 属性内部名称 */
  private String typeInternalName;

  /** 命中 */
  private Integer accuracy;

  /** 此招式效果发生的概率百分比值 */
  private Integer effectChance;

  /** 招式点数 */
  private Integer pp;

  /** -8到8之间的值。设置战斗中招式执行的顺序 */
  private Integer priority;

  /** 威力 */
  private Integer power;

  /** 此招式对目标造成的伤害类型(内部名称) */
  private String damageClassInternalName;

  /** 接收攻击效果的目标类型(内部名称) */
  private String targetInternalName;

  /** 文本描述 */
  private String text;

  /** 招式简要效果描述 */
  private String shortEffect;

  /** 招式效果描述 */
  private String effect;

  /** 此招式持续生效的最小回合数。如果总是只持续一回合，则为空 */
  private Integer minHits;

  /** 此招式持续生效的最小回合数。如果总是只持续一回合，则为空 */
  private Integer maxTurns;

  /** HP吸取（如果为正）或反作用伤害（如果为负），以造成伤害的百分比表示 */
  private Integer drain;

  /** 攻击方宝可梦恢复的HP量，以其最大HP的百分比表示 */
  private Integer healing;

  /** 暴击率加成 */
  private Integer critRate;

  /** 此攻击导致状态异常的可能性 */
  private Integer ailmentChance;

  /** 此攻击导致目标宝可梦畏缩的可能性 */
  private Integer flinchChance;

  /** 此攻击导致目标宝可梦能力值变化的可能性 */
  private Integer statChance;

  /** 招式分类(内部名称) */
  private String categoryInternalName;

  /** 招式导致的状态异常(内部名称) */
  private String ailmentInternalName;
}
