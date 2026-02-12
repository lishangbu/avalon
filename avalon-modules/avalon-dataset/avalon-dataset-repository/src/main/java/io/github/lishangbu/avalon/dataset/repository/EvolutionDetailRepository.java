package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.EvolutionDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/// 进化细节(EvolutionDetail)数据访问层
///
/// @author lishangbu
/// @since 2026/2/12
@Repository
public interface EvolutionDetailRepository extends JpaRepository<EvolutionDetail, Long> {}
