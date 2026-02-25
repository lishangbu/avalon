package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.EvolutionChain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/// 进化链(EvolutionChain)数据访问层
///
/// 提供基础的 CRUD 操作
///
/// @author lishangbu
/// @since 2026/2/12
@Repository
public interface EvolutionChainRepository
        extends JpaRepository<EvolutionChain, Long>, JpaSpecificationExecutor<EvolutionChain> {}
