package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.Move;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;

/**
 * 招式(Move)数据访问层
 *
 * @author lishangbu
 * @since 2025/09/14
 */
@Repository
public interface MoveRepository
    extends ListCrudRepository<Move, Integer>, ListPagingAndSortingRepository<Move, Integer> {}
