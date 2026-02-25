package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/// 树果风味(BerryFlavor)实体类
///
/// 表示树果的风味信息，用于描述宝可梦食用时的风味效果
///
/// @author lishangbu
/// @since 2025/08/20
@Data
@Entity
@Table(comment = "树果风味")
public class BerryFlavor implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    /// 主键
    @Id
    @Flex
    @Column(comment = "主键")
    private Long id;

    /// 内部名称
    @Column(comment = "内部名称", length = 100)
    private String internalName;

    /// 树果风味名称
    @Column(comment = "树果风味名称", length = 100)
    private String name;
}
