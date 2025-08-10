package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.Berry;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;

/**
 * 树果数据存储
 *
 * @author lishangbu
 * @since 2025/5/22
 */
@Repository
public interface BerryRepository extends ListPagingAndSortingRepository<Berry, Long> {}
