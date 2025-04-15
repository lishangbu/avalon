package io.github.lishangbu.avalon.dataset.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lishangbu.avalon.dataset.configuration.DatasetRepositoryTestEnvironmentConfiguration;
import io.github.lishangbu.avalon.dataset.entity.EggGroup;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author lishangbu
 * @since 2025/4/15
 */
@ContextConfiguration(classes = DatasetRepositoryTestEnvironmentConfiguration.class)
@DataJpaTest
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
class EggGroupRepositoryTest {
  @Autowired private EggGroupRepository eggGroupRepository;

  @Test
  public void testCount() {
    assertEquals(15, eggGroupRepository.count());
  }

  @Test
  public void testFindByName() {
    Optional<EggGroup> eggGroupOptional = eggGroupRepository.findByGroup("Monster");
    assertTrue(eggGroupOptional.isPresent());
    EggGroup eggGroup = eggGroupOptional.get();
    assertEquals("Monster", eggGroup.getGroup());
    assertEquals("怪兽", eggGroup.getName());
    assertEquals("像是怪兽一样，或者比较野性。", eggGroup.getText());
    assertEquals("这个蛋群的宝可梦大多原型基于特摄影片中的怪兽以及爬行动物。", eggGroup.getCharacteristics());
  }
}
