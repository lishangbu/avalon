package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.EncounterCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/// 遭遇条件(EncounterCondition)数据访问层
///
/// 提供基础的 CRUD 操作
///
/// @author lishangbu
/// @since 2026/2/12
@Repository
public interface EncounterConditionRepository
        extends JpaRepository<EncounterCondition, Long>,
                JpaSpecificationExecutor<EncounterCondition> {}
