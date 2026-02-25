package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/// 进化触发器(EvolutionTrigger)实体类
///
/// 表示导致宝可梦进化的触发条件或事件
///
/// @author lishangbu
/// @since 2026/2/12
@Data
@Entity
@Table(comment = "进化触发器")
public class EvolutionTrigger implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    /// 主键
    @Id
    @Flex
    @Column(comment = "主键")
    private Long id;

    /// 内部名称
    @Column(comment = "内部名称", length = 100)
    private String internalName;

    /// 触发器名称
    @Column(comment = "触发器名称", length = 100)
    private String name;
}
