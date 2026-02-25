package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.BerryFlavor;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;

/// 树果风味数据访问层测试
class BerryFlavorRepositoryTest extends AbstractRepositoryTest {

    @Resource private BerryFlavorRepository berryFlavorRepository;

    /// 验证 通过ID查询
    @Test
    void shouldFindBerryFlavorById() {
        // Act
        Optional<BerryFlavor> flavorOptional = berryFlavorRepository.findById(1L);
        Assertions.assertTrue(flavorOptional.isPresent());
        BerryFlavor flavor = flavorOptional.get();
        // Assert
        Assertions.assertNotNull(flavor, "应已插入 id=1 的 属性为辣的树果风味记录");
        Assertions.assertEquals(1L, flavor.getId());
        Assertions.assertEquals("spicy", flavor.getInternalName());
        Assertions.assertEquals("辣", flavor.getName());
    }

    /// 验证 selectList 的动态查询能够基于部分 internalName 匹配到 数据库中的数据
    @Test
    void shouldSelectListMatchByInternalName() {
        // Arrange - 构造查询条件，使用部分 internalName
        BerryFlavor cond = new BerryFlavor();
        cond.setInternalName("spicy");

        // Act
        List<BerryFlavor> results = berryFlavorRepository.findAll(Example.of(cond));

        // Assert
        Assertions.assertNotNull(results);
        Assertions.assertFalse(results.isEmpty(), "应至少匹配到一条属性为辣的树果风味记录");
        Assertions.assertTrue(results.stream().anyMatch(r -> "spicy".equals(r.getInternalName())));
    }

    /// 验证 selectList 在空条件下返回所有预加载的记录
    @Test
    void shouldReturnAllPreloadedBerryFlavorsWhenNoCondition() {
        // Act
        List<BerryFlavor> all = berryFlavorRepository.findAll();

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
        berryFlavorRepository.save(bf);

        // Assert
        Assertions.assertNotNull(bf.getId());
        Optional<BerryFlavor> berryFlavorOptional = berryFlavorRepository.findById(bf.getId());
        Assertions.assertTrue(berryFlavorOptional.isPresent());
        BerryFlavor berryFlavor = berryFlavorOptional.get();
        Assertions.assertNotNull(berryFlavor);
        Assertions.assertEquals("savory", berryFlavor.getInternalName());
        Assertions.assertEquals("鲜美", berryFlavor.getName());
    }

    /// 测试根据 ID 更新树果风味的名称
    @Test
    void shouldUpdateBerryFlavorById() {
        // Arrange - 先插入一条临时记录
        BerryFlavor bf = new BerryFlavor();
        bf.setInternalName("fishy");
        bf.setName("鱼腥");
        berryFlavorRepository.saveAndFlush(bf);
        Long id = bf.getId();

        // 修改名称
        bf.setName("鱼腥味");

        // Act
        berryFlavorRepository.saveAndFlush(bf);

        // Assert
        Optional<BerryFlavor> updatedEntityOptional = berryFlavorRepository.findById(id);
        Assertions.assertTrue(updatedEntityOptional.isPresent());
        BerryFlavor updatedEntity = updatedEntityOptional.get();
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
        berryFlavorRepository.saveAndFlush(bf);
        Long id = bf.getId();

        // Act
        berryFlavorRepository.deleteById(id);
        berryFlavorRepository.flush();
        // 验证通过 internalName 查询不到该记录
        BerryFlavor cond = new BerryFlavor();
        cond.setInternalName("pungent");
        List<BerryFlavor> results = berryFlavorRepository.findAll(Example.of(cond));
        Assertions.assertTrue(results.isEmpty(), "删除后按 internalName 查询应返回空集合");
    }
}
