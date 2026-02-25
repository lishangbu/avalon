package io.github.lishangbu.avalon.dataset.repository;

import io.github.lishangbu.avalon.dataset.entity.BerryFirmness;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;

/// 树果硬度数据访问层测试
class BerryFirmnessRepositoryTest extends AbstractRepositoryTest {

    @Resource private BerryFirmnessRepository berryFirmnessRepository;

    /// 验证 数据库中的数据可以通过 selectById 查询到
    @Test
    void shouldFindBerryFirmnessById() {
        // Act
        Optional<BerryFirmness> firmnessOptional = berryFirmnessRepository.findById(1L);

        // Assert
        Assertions.assertTrue(firmnessOptional.isPresent(), "应已插入 id=1 的 属性为很柔软的树果硬度记录");
        BerryFirmness berryFirmness = firmnessOptional.get();
        Assertions.assertEquals(1L, berryFirmness.getId());
        Assertions.assertEquals("very-soft", berryFirmness.getInternalName());
        Assertions.assertEquals("很柔软", berryFirmness.getName());
    }

    /// 验证 selectList 的动态查询能够基于 internalName 匹配到预加载的数据
    @Test
    void shouldSelectListMatchByInternalName() {
        BerryFirmness cond = new BerryFirmness();
        cond.setInternalName("hard");

        List<BerryFirmness> results = berryFirmnessRepository.findAll(Example.of(cond));

        Assertions.assertNotNull(results);
        Assertions.assertFalse(results.isEmpty(), "应至少匹配到一条属性为坚硬的树果硬度记录");
        Assertions.assertTrue(results.stream().anyMatch(r -> r.getInternalName().contains("hard")));
    }

    /// 验证在空条件下能返回预加载的所有记录
    @Test
    void shouldReturnAllPreloadedWhenNoCondition() {
        List<BerryFirmness> all = berryFirmnessRepository.findAll();
        Assertions.assertNotNull(all);
        Assertions.assertTrue(all.size() >= 5, "预期至少有 5 条预加载的树果硬度记录");
    }

    /// 测试插入树果硬度并查询
    @Test
    void shouldInsertBerryFirmness() {
        BerryFirmness bf = new BerryFirmness();
        bf.setInternalName("moderate");
        bf.setName("中等");

        berryFirmnessRepository.save(bf);
        Assertions.assertNotNull(bf.getId());

        Optional<BerryFirmness> firmnessOptional = berryFirmnessRepository.findById(bf.getId());
        Assertions.assertTrue(firmnessOptional.isPresent());
        BerryFirmness berryFirmness = firmnessOptional.get();
        Assertions.assertEquals("moderate", berryFirmness.getInternalName());
        Assertions.assertEquals("中等", berryFirmness.getName());
    }

    /// 测试更新树果硬度
    @Test
    void shouldUpdateBerryFirmnessById() {
        BerryFirmness bf = new BerryFirmness();
        bf.setInternalName("tender");
        bf.setName("嫩");
        berryFirmnessRepository.save(bf);
        Long id = bf.getId();
        bf.setName("非常嫩");
        berryFirmnessRepository.saveAndFlush(bf);
        Optional<BerryFirmness> updatedEntityOptional = berryFirmnessRepository.findById(id);
        Assertions.assertTrue(updatedEntityOptional.isPresent());
        BerryFirmness updatedEntity = updatedEntityOptional.get();
        Assertions.assertEquals("非常嫩", updatedEntity.getName());
    }

    /// 测试删除树果硬度
    @Test
    void shouldDeleteBerryFirmnessById() {
        BerryFirmness bf = new BerryFirmness();
        bf.setInternalName("ephemeral");
        bf.setName("短暂");
        berryFirmnessRepository.save(bf);
        Long id = bf.getId();

        berryFirmnessRepository.deleteById(id);
        berryFirmnessRepository.flush();

        // 通过 internalName 查询应该为空
        BerryFirmness cond = new BerryFirmness();
        cond.setInternalName("ephemeral");
        List<BerryFirmness> results = berryFirmnessRepository.findAll(Example.of(cond));
        Assertions.assertTrue(results.isEmpty(), "删除后按 internalName 查询应返回空集合");
    }
}
