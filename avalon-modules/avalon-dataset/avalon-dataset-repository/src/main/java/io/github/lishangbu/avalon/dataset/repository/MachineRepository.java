package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.Machine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 招式机(Machine)数据访问层
 *
 * @author lishangbu
 * @since 2025/09/14
 */
@Repository
public interface MachineRepository extends JpaRepository<Machine, Long> {}
