package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/// 道具属性(ItemAttribute)实体类
///
/// 表示道具的属性定义，例如是否可在战斗中使用等
///
/// @author lishangbu
/// @since 2025/08/20
@Data
@Entity
@Table(comment = "道具属性")
public class ItemAttribute implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    /// 主键
    @Id
    @Flex
    @Column(comment = "主键")
    private Long id;

    /// 内部名称
    @Column(comment = "内部名称", length = 100)
    private String internalName;

    /// 道具属性名称
    @Column(comment = "道具属性名称", length = 100)
    private String name;

    /// 道具属性描述
    @Column(comment = "道具属性描述", length = 100)
    private String description;
}
