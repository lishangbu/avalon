package io.github.lishangbu.avalon.dataset.mapper;

import com.baomidou.mybatisplus.test.autoconfigure.MybatisPlusTest;
import io.github.lishangbu.avalon.dataset.AbstractMapperTest;
import io.github.lishangbu.avalon.dataset.entity.BerryFirmness;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/// 树果硬度数据访问层测试
@MybatisPlusTest
class BerryFirmnessMapperTest extends AbstractMapperTest {

  @Resource private BerryFirmnessMapper berryFirmnessMapper;

  /// 验证 Liquibase 导入的数据可以通过 selectById 查询到（使用 CSV 中已知的 ID）
  @Test
  void shouldFindBerryFirmnessByIdFromLiquibase() {
    // Act
    BerryFirmness firmness = berryFirmnessMapper.selectById(1L);

    // Assert
    Assertions.assertNotNull(firmness, "Liquibase 应已插入 id=1 的 属性为很柔软的树果硬度记录");
    Assertions.assertEquals(1L, firmness.getId());
    Assertions.assertEquals("very-soft", firmness.getInternalName());
    Assertions.assertEquals("很柔软", firmness.getName());
  }

  /// 验证 selectList 的动态查询能够基于 internalName 匹配到预加载的数据
  @Test
  void shouldSelectListMatchByInternalName() {
    BerryFirmness cond = new BerryFirmness();
    cond.setInternalName("hard");

    List<BerryFirmness> results = berryFirmnessMapper.selectList(cond);

    Assertions.assertNotNull(results);
    Assertions.assertFalse(results.isEmpty(), "应至少匹配到一条属性为坚硬的树果硬度记录");
    Assertions.assertTrue(results.stream().anyMatch(r -> r.getInternalName().contains("hard")));
  }

  /// 验证在空条件下能返回预加载的所有记录
  @Test
  void shouldReturnAllPreloadedWhenNoCondition() {
    List<BerryFirmness> all = berryFirmnessMapper.selectList(new BerryFirmness());
    Assertions.assertNotNull(all);
    Assertions.assertTrue(all.size() >= 5, "预期至少有 5 条预加载的树果硬度记录");
  }

  /// 测试插入树果硬度并查询
  @Test
  void shouldInsertBerryFirmness() {
    BerryFirmness bf = new BerryFirmness();
    bf.setInternalName("moderate");
    bf.setName("中等");

    int inserted = berryFirmnessMapper.insert(bf);
    Assertions.assertEquals(1, inserted);
    Assertions.assertNotNull(bf.getId());

    BerryFirmness found = berryFirmnessMapper.selectById(bf.getId());
    Assertions.assertNotNull(found);
    Assertions.assertEquals("moderate", found.getInternalName());
    Assertions.assertEquals("中等", found.getName());
  }

  /// 测试更新树果硬度
  @Test
  void shouldUpdateBerryFirmnessById() {
    BerryFirmness bf = new BerryFirmness();
    bf.setInternalName("tender");
    bf.setName("嫩");
    berryFirmnessMapper.insert(bf);
    Long id = bf.getId();

    bf.setName("非常嫩");
    int updated = berryFirmnessMapper.updateById(bf);
    Assertions.assertEquals(1, updated);
    BerryFirmness updatedEntity = berryFirmnessMapper.selectById(id);
    Assertions.assertNotNull(updatedEntity);
    Assertions.assertEquals("非常嫩", updatedEntity.getName());
  }

  /// 测试删除树果硬度
  @Test
  void shouldDeleteBerryFirmnessById() {
    BerryFirmness bf = new BerryFirmness();
    bf.setInternalName("ephemeral");
    bf.setName("短暂");
    berryFirmnessMapper.insert(bf);
    Long id = bf.getId();

    int deleted = berryFirmnessMapper.deleteById(id);
    Assertions.assertEquals(1, deleted);

    // 通过 internalName 查询应该为空
    BerryFirmness cond = new BerryFirmness();
    cond.setInternalName("ephemeral");
    List<BerryFirmness> results = berryFirmnessMapper.selectList(cond);
    Assertions.assertTrue(results.isEmpty(), "删除后按 internalName 查询应返回空集合");
  }
}

