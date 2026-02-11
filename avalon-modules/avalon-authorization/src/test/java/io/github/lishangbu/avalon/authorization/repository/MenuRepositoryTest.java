package io.github.lishangbu.avalon.authorization.repository;

import io.github.lishangbu.avalon.authorization.entity.Menu;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

/// 菜单 Repository 测试
///
/// 验证菜单查询、插入、更新与删除流程，依赖数据库初始化的测试数据
///
/// @author lishangbu
/// @since 2025/12/6
@Transactional(rollbackFor = Exception.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MenuRepositoryTest extends AbstractRepositoryTest {
  @Resource private MenuRepository menuRepository;

  private static Long insertId;

  @Test
  @Order(1)
  void testSelectMenuById() {
    Optional<Menu> menuOptional = menuRepository.findById(1L);
    Assertions.assertTrue(menuOptional.isPresent());
    Menu menu = menuOptional.get();
    Assertions.assertEquals("dashboard", menu.getKey());
    Assertions.assertEquals("仪表板", menu.getLabel());
    Assertions.assertEquals(1L, menu.getId());
  }

  @Test
  @Order(2)
  // 确保新增操作后能够提交事务
  @Commit
  void testInsertMenu() {
    Menu menu = new Menu();
    menu.setKey("unit_test_menu");
    menu.setLabel("单元测试菜单");
    menu.setPath("/unit-test");
    menu.setOrder(100);
    menu.setDisabled(false);
    menu.setShow(true);
    menuRepository.save(menu);
    insertId = menu.getId();
  }

  @Test
  @Order(3)
  // 确保更新操作后能够提交事务
  @Commit
  void testUpdateMenuById() {
    Optional<Menu> menuOptional = menuRepository.findById(insertId);
    Assertions.assertTrue(menuOptional.isPresent());
    Menu menu = menuOptional.get();
    menu.setLabel("更新单元测试菜单");
    menu.setDisabled(true);
    menuRepository.save(menu);
  }

  @Test
  @Order(4)
  void testSelectUpdatedMenuById() {
    Optional<Menu> menuOptional = menuRepository.findById(insertId);
    Assertions.assertTrue(menuOptional.isPresent());
    Menu menu = menuOptional.get();
    Assertions.assertEquals("更新单元测试菜单", menu.getLabel());
    Assertions.assertTrue(menu.getDisabled());
  }

  @Test
  @Order(5)
  void testFindAllByRoleCodes() {
    List<Menu> menus = menuRepository.findAllByRoleCodes(List.of("ROLE_SUPER_ADMIN"));
    Assertions.assertNotNull(menus);
    Assertions.assertFalse(menus.isEmpty());
    // 假设 ROLE_TEST 有菜单
  }

  @Test
  @Order(6)
  void testDeleteById() {
    menuRepository.deleteById(insertId);
  }
}
