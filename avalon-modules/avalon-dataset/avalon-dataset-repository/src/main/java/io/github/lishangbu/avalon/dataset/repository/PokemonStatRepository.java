package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.PokemonStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/// 宝可梦能力值数据访问接口
///
/// 提供宝可梦能力值数据的CRUD操作
///
/// @author lishangbu
/// @since 2026/2/16
@Repository
public interface PokemonStatRepository
        extends JpaRepository<PokemonStat, PokemonStat.PokemonStatId> {}
