package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.MoveAilment;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;

/**
 * 招式异常(MoveAilment)数据访问层
 *
 * @author lishangbu
 * @since 2025/09/14
 */
@Repository
public interface MoveAilmentRepository
    extends ListCrudRepository<MoveAilment, Long>,
        ListPagingAndSortingRepository<MoveAilment, Long> {}
