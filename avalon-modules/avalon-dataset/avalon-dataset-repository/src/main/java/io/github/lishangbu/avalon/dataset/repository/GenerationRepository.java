package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.Generation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 世代数据存储
 *
 * @author lishangbu
 * @since 2025/4/14
 */
@Repository
public interface GenerationRepository extends JpaRepository<Generation, Integer> {

  Optional<Generation> findByName(String name);
}
