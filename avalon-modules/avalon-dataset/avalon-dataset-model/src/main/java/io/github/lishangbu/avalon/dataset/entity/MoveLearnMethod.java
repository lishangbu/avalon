package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/// 学习招式的方法(MoveLearnMethod)实体类
///
/// 表示宝可梦学习招式的方式（例如升级、遗传、TM 等）
///
/// @author lishangbu
/// @since 2025/08/20
@Data
@Entity
@Table(comment = "学习招式的方法")
public class MoveLearnMethod implements Serializable {
    @Serial private static final long serialVersionUID = 240573508164457984L;

    /// 主键
    @Id
    @Flex
    @Column(comment = "主键")
    private Long id;

    /// 学习招式的方法内部名称
    @Column(comment = "学习招式的方法内部名称", length = 100)
    private String internalName;

    /// 学习招式的方法名称
    @Column(comment = "学习招式的方法名称", length = 100)
    private String name;

    /// 学习招式的方法的描述
    @Column(comment = "学习招式的方法的描述", length = 300)
    private String description;
}
