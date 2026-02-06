package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.MoveDamageClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/// 招式伤害类别(MoveDamageClass)数据访问层
///
/// 提供基础的 CRUD 操作
///
/// @author lishangbu
/// @since 2025/09/14
@Repository
public interface MoveDamageClassRepository
    extends JpaRepository<MoveDamageClass, Long>, JpaSpecificationExecutor<MoveDamageClass> {}
