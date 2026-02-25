package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/// 宝可梦颜色(PokemonColor)实体类
///
/// 表示宝可梦的颜色信息，用于图鉴分类
///
/// @author lishangbu
/// @since 2026/2/12
@Data
@Entity
@Table(comment = "宝可梦颜色")
public class PokemonColor implements Serializable {
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
}
