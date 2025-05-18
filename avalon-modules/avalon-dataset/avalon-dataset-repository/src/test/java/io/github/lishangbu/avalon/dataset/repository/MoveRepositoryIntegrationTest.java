package io.github.lishangbu.avalon.dataset.repository;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lishangbu.avalon.dataset.configuration.DatasetRepositoryTestEnvironmentConfiguration;
import io.github.lishangbu.avalon.dataset.entity.Move;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * 招式数据集成测试
 *
 * @author lishangbu
 * @since 2025/4/14
 */
@ContextConfiguration(classes = DatasetRepositoryTestEnvironmentConfiguration.class)
@DataJpaTest
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
class MoveRepositoryIntegrationTest {
  @Autowired private MoveRepository moveRepository;

  @Autowired private TypeRepository typeRepository;

  @Autowired private MoveCategoryRepository moveCategoryRepository;

  @Test
  public void testSave() {

    Move move = new Move();
    move.setId("36");
    move.setName("猛撞");
    move.setCode("Take Down");
    moveCategoryRepository.findByName("物理").ifPresent(move::setCategory);
    typeRepository.findByType("Normal").ifPresent(move::setType);
    move.setPower(90);
    move.setAccuracy(85);
    move.setPp(20);
    move.setText("以惊人的气势撞向对手进行攻击。自己也会受到少许伤害。");
    move.setEffect("攻击目标造成伤害。 \n");
    moveRepository.saveAndFlush(move);

    assertTrue(moveRepository.findById(36).isPresent());
  }
}
