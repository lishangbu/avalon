package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.PokemonType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 宝可梦属性(PokemonType)数据访问层
 *
 * @author lishangbu
 * @since 2025/09/14
 */
@Repository
public interface PokemonTypeRepository extends JpaRepository<PokemonType, Long> {}
