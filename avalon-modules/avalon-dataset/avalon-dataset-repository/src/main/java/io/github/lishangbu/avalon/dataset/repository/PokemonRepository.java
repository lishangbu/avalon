package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.Pokemon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 宝可梦(Pokemon)数据访问层
 *
 * @author lishangbu
 * @since 2025/09/14
 */
@Repository
public interface PokemonRepository extends JpaRepository<Pokemon, Long> {}
