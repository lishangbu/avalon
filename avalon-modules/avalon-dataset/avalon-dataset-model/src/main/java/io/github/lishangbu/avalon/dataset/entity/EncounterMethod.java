package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/// 遭遇方法(EncounterMethod)实体类
///
/// 表示玩家在野外遇到宝可梦的方式
///
/// @author lishangbu
/// @since 2026/2/12
@Data
@Entity
@Table(comment = "遭遇方法")
public class EncounterMethod implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    /// 主键
    @Id
    @Flex
    @Column(comment = "主键")
    private Long id;

    /// 内部名称
    @Column(comment = "内部名称", length = 100)
    private String internalName;

    /// 方法名称
    @Column(comment = "方法名称", length = 100)
    private String name;

    /// 排序顺序
    @Column(comment = "排序顺序")
    private Integer sortingOrder;
}
