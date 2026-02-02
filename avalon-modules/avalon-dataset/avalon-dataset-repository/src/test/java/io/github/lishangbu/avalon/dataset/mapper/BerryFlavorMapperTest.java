package io.github.lishangbu.avalon.dataset.mapper;

import com.baomidou.mybatisplus.test.autoconfigure.MybatisPlusTest;
import io.github.lishangbu.avalon.dataset.AbstractMapperTest;
import io.github.lishangbu.avalon.dataset.entity.BerryFlavor;
import jakarta.annotation.Resource;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/// 树果风味数据访问层测试
@MybatisPlusTest
class BerryFlavorMapperTest extends AbstractMapperTest {

  @Resource private BerryFlavorMapper berryFlavorMapper;

  /// 验证 Liquibase 导入的数据可以通过 selectById 查询到（使用已知 CSV 中的 ID）
  @Test
  void shouldFindBerryFlavorByIdFromLiquibase() {
    // Act
    BerryFlavor flavor = berryFlavorMapper.selectById(1L);

    // Assert
    Assertions.assertNotNull(flavor, "Liquibase 应已插入 id=1 的 属性为辣的树果风味记录");
    Assertions.assertEquals(1L, flavor.getId());
    Assertions.assertEquals("spicy", flavor.getInternalName());
    Assertions.assertEquals("辣", flavor.getName());
  }

  /// 验证 selectList 的动态查询能够基于部分 internalName 匹配到 Liquibase 中的数据
  @Test
  void shouldSelectListMatchByInternalName() {
    // Arrange - 构造查询条件，使用部分 internalName
    BerryFlavor cond = new BerryFlavor();
    cond.setInternalName("spicy");

    // Act
    List<BerryFlavor> results = berryFlavorMapper.selectList(cond);

    // Assert
    Assertions.assertNotNull(results);
    Assertions.assertFalse(results.isEmpty(), "应至少匹配到一条属性为辣的树果风味记录");
    Assertions.assertTrue(results.stream().anyMatch(r -> "spicy".equals(r.getInternalName())));
  }

  /// 验证 selectList 在空条件下返回所有预加载的记录
  @Test
  void shouldReturnAllPreloadedBerryFlavorsWhenNoCondition() {
    // Act
    List<BerryFlavor> all = berryFlavorMapper.selectList(new BerryFlavor());

    // Assert
    Assertions.assertNotNull(all);
    // 根据 db/changelog 数据文件，至少有 5 条记录
    Assertions.assertTrue(all.size() >= 5, "预期至少有 5 个预加载的树果风味记录");
  }

  /// 测试插入树果风味（使用简短合理的 internalName 避免与预加载数据冲突）
  @Test
  void shouldInsertBerryFlavor() {
    // Arrange
    BerryFlavor bf = new BerryFlavor();
    bf.setInternalName("savory");
    bf.setName("鲜美");

    // Act
    int inserted = berryFlavorMapper.insert(bf);

    // Assert
    Assertions.assertEquals(1, inserted);
    Assertions.assertNotNull(bf.getId());
    BerryFlavor found = berryFlavorMapper.selectById(bf.getId());
    Assertions.assertNotNull(found);
    Assertions.assertEquals("savory", found.getInternalName());
    Assertions.assertEquals("鲜美", found.getName());
  }

  /// 测试根据 ID 更新树果风味的名称
  @Test
  void shouldUpdateBerryFlavorById() {
    // Arrange - 先插入一条临时记录
    BerryFlavor bf = new BerryFlavor();
    bf.setInternalName("fishy");
    bf.setName("鱼腥");
    berryFlavorMapper.insert(bf);
    Long id = bf.getId();

    // 修改名称
    bf.setName("鱼腥味");

    // Act
    int updated = berryFlavorMapper.updateById(bf);

    // Assert
    Assertions.assertEquals(1, updated);
    BerryFlavor updatedEntity = berryFlavorMapper.selectById(id);
    Assertions.assertNotNull(updatedEntity);
    Assertions.assertEquals("鱼腥味", updatedEntity.getName());
  }

  /// 测试根据 ID 删除树果风味
  @Test
  void shouldDeleteBerryFlavorById() {
    // Arrange - 插入临时记录
    BerryFlavor bf = new BerryFlavor();
    bf.setInternalName("pungent");
    bf.setName("刺激性风味");
    berryFlavorMapper.insert(bf);
    Long id = bf.getId();

    // Act
    int deleted = berryFlavorMapper.deleteById(id);

    // Assert
    Assertions.assertEquals(1, deleted);
    // 验证通过 internalName 查询不到该记录
    BerryFlavor cond = new BerryFlavor();
    cond.setInternalName("pungent");
    List<BerryFlavor> results = berryFlavorMapper.selectList(cond);
    Assertions.assertTrue(results.isEmpty(), "删除后按 internalName 查询应返回空集合");
  }
}
