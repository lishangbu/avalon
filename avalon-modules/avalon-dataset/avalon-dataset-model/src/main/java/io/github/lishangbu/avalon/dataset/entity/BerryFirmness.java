package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/// 树果硬度(BerryFirmness)实体类
///
/// 表示树果的硬度分类，用于描述树果的质地
///
/// @author lishangbu
/// @since 2025/08/20
@Data
@Entity
@Table(comment = "树果硬度")
public class BerryFirmness implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    /// 主键
    @Id
    @Flex
    @Column(comment = "主键")
    private Long id;

    /// 内部名称
    @Column(comment = "内部名称", length = 100)
    private String internalName;

    /// 树果硬度名称
    @Column(comment = "树果硬度名称", length = 100)
    private String name;
}
