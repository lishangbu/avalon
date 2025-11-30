package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.Pokemon;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;

/**
 * 宝可梦(Pokemon)数据访问层
 *
 * @author lishangbu
 * @since 2025/09/14
 */
@Repository
public interface PokemonRepository
    extends ListCrudRepository<Pokemon, Long>, ListPagingAndSortingRepository<Pokemon, Long> {}
