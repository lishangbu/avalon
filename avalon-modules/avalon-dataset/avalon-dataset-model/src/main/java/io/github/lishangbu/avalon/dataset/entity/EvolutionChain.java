package io.github.lishangbu.avalon.dataset.entity;

import io.github.lishangbu.avalon.hibernate.Flex;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import lombok.Data;

/// 进化链(EvolutionChain)实体类
///
/// 表示从基本形态到最终进化形态的完整宝可梦进化路径
///
/// @author lishangbu
/// @since 2026/2/12
@Data
@Entity
@Table(comment = "进化链")
public class EvolutionChain implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    /// 主键
    @Id
    @Flex
    @Column(comment = "主键")
    private Long id;

    /// 幼年触发道具 ID
    @Column(comment = "幼年触发道具ID")
    private Long babyTriggerItemId;
}
