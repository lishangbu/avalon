package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.ItemAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 道具属性(ItemAttribute)数据访问层
 *
 * @author lishangbu
 * @since 2025/09/14
 */
@Repository
public interface ItemAttributeRepository extends JpaRepository<ItemAttribute, Long> {}
