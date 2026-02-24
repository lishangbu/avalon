package io.github.lishangbu.avalon.game.calculator.growthrate;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

class GrowthRateCalculatorFactoryTest {

  private final JsonMapper jsonMapper = JsonMapper.builder().build();

  // 直接构造 factory，注入所有实现
  private final GrowthRateCalculatorFactory growthRateService =
      new GrowthRateCalculatorFactory(
          List.of(
              new SlowGrowthRateCalculator(),
              new MediumGrowthRateCalculator(),
              new FastGrowthRateCalculator(),
              new MediumSlowGrowthRateCalculator(),
              new SlowThenVeryFastGrowthRateCalculator(),
              new FastThenVerySlowGrowthRateCalculator()));

  /// 测试calculateGrowthRate方法，验证slow类型的计算结果
  ///
  /// 场景：读取resources/slow.json，对每个等级验证计算结果
  /// 输入：internalName="slow", level从1到100
  /// 预期：返回对应experience值
  @Test
  void calculateGrowthRate_shouldReturnCorrectValueForSlow() throws Exception {
    validateGrowthRate("slow", "slow.json");
  }

  /// 测试calculateGrowthRate方法，验证medium类型的计算结果
  ///
  /// 场景：读取resources/medium.json，对每个等级验证计算结果
  /// 输入：internalName="medium", level从1到100
  /// 预期：返回对应experience值
  @Test
  void calculateGrowthRate_shouldReturnCorrectValueForMedium() throws Exception {
    validateGrowthRate("medium", "medium.json");
  }

  /// 测试calculateGrowthRate方法，验证fast类型的计算结果
  ///
  /// 场景：读取resources/fast.json，对每个等级验证计算结果
  /// 输入：internalName="fast", level从1到100
  /// 预期：返回对应experience值
  @Test
  void calculateGrowthRate_shouldReturnCorrectValueForFast() throws Exception {
    validateGrowthRate("fast", "fast.json");
  }

  /// 测试calculateGrowthRate方法，验证medium-slow类型的计算结果
  ///
  /// 场景：读取resources/medium-slow.json，对每个等级验证计算结果
  /// 输入：internalName="medium-slow", level从1到100
  /// 预期：返回对应experience值
  @Test
  void calculateGrowthRate_shouldReturnCorrectValueForMediumSlow() throws Exception {
    validateGrowthRate("medium-slow", "medium-slow.json");
  }

  /// 测试calculateGrowthRate方法，验证slow-then-very-fast类型的计算结果
  ///
  /// 场景：读取resources/slow-then-very-fast.json，对每个等级验证计算结果
  /// 输入：internalName="slow-then-very-fast", level从1到100
  /// 预期：返回对应experience值
  @Test
  void calculateGrowthRate_shouldReturnCorrectValueForSlowThenVeryFast() throws Exception {
    validateGrowthRate("slow-then-very-fast", "slow-then-very-fast.json");
  }

  /// 测试calculateGrowthRate方法，验证fast-then-very-slow类型的计算结果
  ///
  /// 场景：读取resources/fast-then-very-slow.json，对每个等级验证计算结果
  /// 输入：internalName="fast-then-very-slow", level从1到100
  /// 预期：返回对应experience值
  @Test
  void calculateGrowthRate_shouldReturnCorrectValueForFastThenVerySlow() throws Exception {
    validateGrowthRate("fast-then-very-slow", "fast-then-very-slow.json");
  }

  /// 测试calculateGrowthRate方法，验证无效level
  ///
  /// 场景：level <= 0
  /// 输入：internalName="slow", level=0
  /// 预期：返回0
  @Test
  void calculateGrowthRate_shouldReturnZeroForInvalidLevel() {
    Integer result = growthRateService.calculateGrowthRate("slow", 0);

    assertThat(result).isEqualTo(0);
  }

  /// 测试calculateGrowthRate方法，验证未知internalName
  ///
  /// 场景：internalName="unknown"
  /// 输入：id=7, level=10
  /// 预期：返回0
  @Test
  void calculateGrowthRate_shouldReturnZeroForUnknownType() {
    Integer result = growthRateService.calculateGrowthRate("unknown", 10);

    assertThat(result).isEqualTo(0);
  }

  private void validateGrowthRate(String type, String resourceName) throws Exception {
    // 读取JSON文件
    List<GrowthRateValidateResult> growthRateValidateResults =
        jsonMapper.readValue(
            new ClassPathResource(resourceName).getInputStream(), new TypeReference<>() {});
    // 对每个等级验证
    assert growthRateValidateResults != null;
    for (GrowthRateValidateResult growthRateValidateResult : growthRateValidateResults) {
      int level = growthRateValidateResult.level();
      int expectedExperience = growthRateValidateResult.experience();
      Integer result = growthRateService.calculateGrowthRate(type, level);
      assertThat(result).isEqualTo(expectedExperience);
    }
  }

  private record GrowthRateValidateResult(Integer experience, Integer level) {}
}
