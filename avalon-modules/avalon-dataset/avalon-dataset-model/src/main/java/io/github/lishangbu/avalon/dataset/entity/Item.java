package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.Set;
import lombok.Data;

/// 道具(Item)实体类
///
/// 表示游戏中的道具信息，包含标识、内部名称、价格、投掷效果及文本描述
///
/// @author lishangbu
/// @since 2025/08/20
@Data
@Entity
@Table(comment = "道具")
public class Item implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    /// 主键
    @Id
    @Flex
    @Column(comment = "主键")
    private Long id;

    /// 内部名称
    @Column(comment = "内部名称", length = 100)
    private String internalName;

    /// 道具名称
    @Column(comment = "道具名称", length = 100)
    private String name;

    /// 在商店中的价格
    @Column(comment = "在商店中的价格")
    private Integer cost;

    /// 使用此道具进行投掷行动时的威力
    @Column(comment = "使用此道具进行投掷行动时的威力")
    private Integer flingPower;

    /// 使用此道具进行投掷行动时的效果
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "item_fling_effect_id", comment = "使用此道具进行投掷行动时的效果")
    private ItemFlingEffect itemFlingEffect;

    /// 此道具所属的类别
    @ManyToMany
    @JoinTable(
            name = "item_attribute_relation",
            comment = "物品与物品属性的关联表",
            joinColumns = @JoinColumn(name = "item_id"),
            inverseJoinColumns = @JoinColumn(name = "item_attribute_id"))
    private Set<ItemAttribute> itemAttributes;

    /// 简要效果描述
    @Column(comment = "简要效果描述", length = 500)
    private String shortEffect;

    /// 详细效果描述
    @Column(comment = "简要效果描述", length = 1000)
    private String effect;

    /// 道具文本
    @Column(comment = "道具文本", length = 1000)
    private String text;
}
