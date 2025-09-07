package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.Ability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 特性(Ability)数据访问层
 *
 * @author lishangbu
 * @since 2025/09/14
 */
@Repository
public interface AbilityRepository extends JpaRepository<Ability, Long> {}
