package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.Machine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 技能学习机器数据存储
 *
 * @author lishangbu
 * @since 2025/4/14
 */
@Repository
public interface MachineRepository extends JpaRepository<Machine, Integer> {}
