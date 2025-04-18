package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.Ability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 特性数据存储
 *
 * @author lishangbu
 * @since 2025/4/17
 */
@Repository
public interface AbilityRepository extends JpaRepository<Ability, Integer> {}
