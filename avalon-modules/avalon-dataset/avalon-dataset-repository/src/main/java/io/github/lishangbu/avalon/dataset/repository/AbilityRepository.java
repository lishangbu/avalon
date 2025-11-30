package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.Ability;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;

/**
 * 特性(Ability)数据访问层
 *
 * @author lishangbu
 * @since 2025/09/14
 */
@Repository
public interface AbilityRepository
    extends ListCrudRepository<Ability, Long>, ListPagingAndSortingRepository<Ability, Long> {}
