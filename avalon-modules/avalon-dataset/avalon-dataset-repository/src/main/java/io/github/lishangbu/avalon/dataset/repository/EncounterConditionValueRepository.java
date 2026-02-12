package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.EncounterConditionValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/// 遭遇条件值(EncounterConditionValue)数据访问层
///
/// 提供基础的 CRUD 操作
///
/// @author lishangbu
/// @since 2026/2/12
@Repository
public interface EncounterConditionValueRepository
    extends JpaRepository<EncounterConditionValue, Long>,
        JpaSpecificationExecutor<EncounterConditionValue> {}
