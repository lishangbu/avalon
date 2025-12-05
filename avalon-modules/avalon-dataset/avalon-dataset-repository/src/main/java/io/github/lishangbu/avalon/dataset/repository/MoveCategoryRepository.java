package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.MoveCategory;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;

/**
 * 招式类别(MoveCategory)数据访问层
 *
 * @author lishangbu
 * @since 2025/09/14
 */
@Repository
public interface MoveCategoryRepository
    extends ListCrudRepository<MoveCategory, Integer>,
        ListPagingAndSortingRepository<MoveCategory, Integer> {}
