package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/// 遭遇条件(EncounterCondition)实体类
///
/// 表示遭遇宝可梦时的条件，如时间、季节等
///
/// @author lishangbu
/// @since 2026/2/12
@Data
@Entity
@Table(comment = "遭遇条件")
public class EncounterCondition implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    /// 主键
    @Id
    @Flex
    @Column(comment = "主键")
    private Long id;

    /// 内部名称
    @Column(comment = "内部名称", length = 100)
    private String internalName;

    /// 条件名称
    @Column(comment = "条件名称", length = 100)
    private String name;
}
