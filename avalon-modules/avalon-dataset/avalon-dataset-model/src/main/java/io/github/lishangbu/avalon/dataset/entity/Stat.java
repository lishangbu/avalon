package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/// 能力(Stat)实体类
///
/// 表示宝可梦的能力信息，如HP、攻击、防御等
///
/// @author lishangbu
/// @since 2026/2/11
@Data
@Entity
@Table(comment = "能力")
public class Stat implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    /// 主键
    @Id
    @Flex
    @Column(comment = "主键")
    private Long id;

    /// 内部名称
    @Column(comment = "内部名称", length = 100)
    private String internalName;

    /// 显示名称
    @Column(comment = "显示名称", length = 100)
    private String name;

    /// 游戏侧用于此能力的 ID
    @Column(comment = "游戏索引")
    private Integer gameIndex;

    /// 此能力是否仅在战斗中存在
    @Column(comment = "是否仅战斗中存在")
    private Boolean isBattleOnly;

    /// 与此能力相关的伤害类别
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "move_damage_class_id", comment = "招式伤害类别")
    private MoveDamageClass moveDamageClass;
}
