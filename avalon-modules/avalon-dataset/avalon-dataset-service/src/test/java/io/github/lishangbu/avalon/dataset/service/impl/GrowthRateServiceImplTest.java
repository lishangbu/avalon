package io.github.lishangbu.avalon.dataset.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.github.lishangbu.avalon.dataset.entity.GrowthRate;
import io.github.lishangbu.avalon.dataset.repository.GrowthRateRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class GrowthRateServiceImplTest {

  private final JsonMapper jsonMapper = JsonMapper.builder().build();

  @Mock private GrowthRateRepository growthRateRepository;

  @InjectMocks private GrowthRateService growthRateService;

  /// 测试calculateGrowthRate方法，验证slow类型的计算结果
  ///
  /// 场景：读取resources/slow.json，对每个等级验证计算结果
  /// 输入：id=1, level从1到100
  /// 预期：返回对应experience值
  @Test
  void calculateGrowthRate_shouldReturnCorrectValueForSlow() throws Exception {
    validateGrowthRate("slow", 1L, "slow.json");
  }

  /// 测试calculateGrowthRate方法，验证medium类型的计算结果
  ///
  /// 场景：读取resources/medium.json，对每个等级验证计算结果
  /// 输入：id=2, level从1到100
  /// 预期：返回对应experience值
  @Test
  void calculateGrowthRate_shouldReturnCorrectValueForMedium() throws Exception {
    validateGrowthRate("medium", 2L, "medium.json");
  }

  /// 测试calculateGrowthRate方法，验证fast类型的计算结果
  ///
  /// 场景：读取resources/fast.json，对每个等级验证计算结果
  /// 输入：id=3, level从1到100
  /// 预期：返回对应experience值
  @Test
  void calculateGrowthRate_shouldReturnCorrectValueForFast() throws Exception {
    validateGrowthRate("fast", 3L, "fast.json");
  }

  /// 测试calculateGrowthRate方法，验证medium-slow类型的计算结果
  ///
  /// 场景：读取resources/medium-slow.json，对每个等级验证计算结果
  /// 输入：id=4, level从1到100
  /// 预期：返回对应experience值
  @Test
  void calculateGrowthRate_shouldReturnCorrectValueForMediumSlow() throws Exception {
    validateGrowthRate("medium-slow", 4L, "medium-slow.json");
  }

  /// 测试calculateGrowthRate方法，验证slow-then-very-fast类型的计算结果
  ///
  /// 场景：读取resources/slow-then-very-fast.json，对每个等级验证计算结果
  /// 输入：id=5, level从1到100
  /// 预期：返回对应experience值
  @Test
  void calculateGrowthRate_shouldReturnCorrectValueForSlowThenVeryFast() throws Exception {
    validateGrowthRate("slow-then-very-fast", 5L, "slow-then-very-fast.json");
  }

  /// 测试calculateGrowthRate方法，验证fast-then-very-slow类型的计算结果
  ///
  /// 场景：读取resources/fast-then-very-slow.json，对每个等级验证计算结果
  /// 输入：id=6, level从1到100
  /// 预期：返回对应experience值
  @Test
  void calculateGrowthRate_shouldReturnCorrectValueForFastThenVerySlow() throws Exception {
    validateGrowthRate("fast-then-very-slow", 6L, "fast-then-very-slow.json");
  }

  /// 测试calculateGrowthRate方法，验证无效level
  ///
  /// 场景：level <= 0
  /// 输入：id=1, level=0
  /// 预期：返回0
  @Test
  void calculateGrowthRate_shouldReturnZeroForInvalidLevel() {
    Integer result = growthRateService.calculateGrowthRate(1L, 0);

    assertThat(result).isEqualTo(0);
  }

  /// 测试calculateGrowthRate方法，验证未知internalName
  ///
  /// 场景：mock返回未知类型
  /// 输入：id=7, level=10
  /// 预期：返回0
  @Test
  void calculateGrowthRate_shouldReturnZeroForUnknownType() {
    GrowthRate growthRate = new GrowthRate();
    growthRate.setId(7L);
    growthRate.setInternalName("unknown");
    when(growthRateRepository.findById(7L)).thenReturn(Optional.of(growthRate));

    Integer result = growthRateService.calculateGrowthRate(7L, 10);

    assertThat(result).isEqualTo(0);
  }

  /// 测试calculateGrowthRate方法，验证不存在的id
  ///
  /// 场景：repository返回empty
  /// 输入：id=999, level=10
  /// 预期：返回0
  @Test
  void calculateGrowthRate_shouldReturnZeroForNonExistentId() {
    when(growthRateRepository.findById(999L)).thenReturn(Optional.empty());

    Integer result = growthRateService.calculateGrowthRate(999L, 10);

    assertThat(result).isEqualTo(0);
  }

  private void validateGrowthRate(String type, Long id, String resourceName) throws Exception {
    // Mock 数据
    GrowthRate growthRate = new GrowthRate();
    growthRate.setId(id);
    growthRate.setInternalName(type);
    when(growthRateRepository.findById(id)).thenReturn(Optional.of(growthRate));

    // 读取JSON文件
    List<GrowthRateValidateResult> growthRateValidateResults =
        jsonMapper.readValue(
            new ClassPathResource(resourceName).getInputStream(), new TypeReference<>() {});
    // 对每个等级验证
    assert growthRateValidateResults != null;
    for (GrowthRateValidateResult growthRateValidateResult : growthRateValidateResults) {
      int level = growthRateValidateResult.level();
      int expectedExperience = growthRateValidateResult.experience();
      Integer result = growthRateService.calculateGrowthRate(id, level);
      assertThat(result).isEqualTo(expectedExperience);
    }
  }

  private record GrowthRateValidateResult(Integer experience, Integer level) {}
}
