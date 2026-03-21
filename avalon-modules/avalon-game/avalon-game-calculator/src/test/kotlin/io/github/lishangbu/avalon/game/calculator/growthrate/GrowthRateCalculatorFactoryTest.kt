package io.github.lishangbu.avalon.game.calculator.growthrate

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.json.JsonMapper

class GrowthRateCalculatorFactoryTest {
    private val jsonMapper: JsonMapper = JsonMapper.builder().build()

    // 直接构造 factory，注入所有实现
    private val growthRateService: GrowthRateCalculatorFactory =
        GrowthRateCalculatorFactory(
            listOf(
                SlowGrowthRateCalculator(),
                MediumGrowthRateCalculator(),
                FastGrowthRateCalculator(),
                MediumSlowGrowthRateCalculator(),
                SlowThenVeryFastGrowthRateCalculator(),
                FastThenVerySlowGrowthRateCalculator(),
            ),
        )

    /**
     * 测试calculateGrowthRate方法，验证slow类型的计算结果
     *
     * 场景：读取resources/slow.json，对每个等级验证计算结果 输入：internalName="slow", level从1到100 预期：返回对应experience值
     */
    @Test
    fun calculateGrowthRate_shouldReturnCorrectValueForSlow() {
        validateGrowthRate("slow", "slow.json")
    }

    /**
     * 测试calculateGrowthRate方法，验证medium类型的计算结果
     *
     * 场景：读取resources/medium.json，对每个等级验证计算结果 输入：internalName="medium", level从1到100
     * 预期：返回对应experience值
     */
    @Test
    fun calculateGrowthRate_shouldReturnCorrectValueForMedium() {
        validateGrowthRate("medium", "medium.json")
    }

    /**
     * 测试calculateGrowthRate方法，验证fast类型的计算结果
     *
     * 场景：读取resources/fast.json，对每个等级验证计算结果 输入：internalName="fast", level从1到100 预期：返回对应experience值
     */
    @Test
    fun calculateGrowthRate_shouldReturnCorrectValueForFast() {
        validateGrowthRate("fast", "fast.json")
    }

    /**
     * 测试calculateGrowthRate方法，验证medium-slow类型的计算结果
     *
     * 场景：读取resources/medium-slow.json，对每个等级验证计算结果 输入：internalName="medium-slow", level从1到100
     * 预期：返回对应experience值
     */
    @Test
    fun calculateGrowthRate_shouldReturnCorrectValueForMediumSlow() {
        validateGrowthRate("medium-slow", "medium-slow.json")
    }

    /**
     * 测试calculateGrowthRate方法，验证slow-then-very-fast类型的计算结果
     *
     * 场景：读取resources/slow-then-very-fast.json，对每个等级验证计算结果 输入：internalName="slow-then-very-fast",
     * level从1到100 预期：返回对应experience值
     */
    @Test
    fun calculateGrowthRate_shouldReturnCorrectValueForSlowThenVeryFast() {
        validateGrowthRate("slow-then-very-fast", "slow-then-very-fast.json")
    }

    /**
     * 测试calculateGrowthRate方法，验证fast-then-very-slow类型的计算结果
     *
     * 场景：读取resources/fast-then-very-slow.json，对每个等级验证计算结果 输入：internalName="fast-then-very-slow",
     * level从1到100 预期：返回对应experience值
     */
    @Test
    fun calculateGrowthRate_shouldReturnCorrectValueForFastThenVerySlow() {
        validateGrowthRate("fast-then-very-slow", "fast-then-very-slow.json")
    }

    /**
     * 测试calculateGrowthRate方法，验证无效level
     *
     * 场景：level <= 0 输入：internalName="slow", level=0 预期：返回0
     */
    @Test
    fun calculateGrowthRate_shouldReturnZeroForInvalidLevel() {
        val result: Int = growthRateService.calculateGrowthRate("slow", 0)

        assertThat(result).isZero()
    }

    /**
     * 测试calculateGrowthRate方法，验证未知internalName
     *
     * 场景：internalName="unknown" 输入：id=7, level=10 预期：返回0
     */
    @Test
    fun calculateGrowthRate_shouldReturnZeroForUnknownType() {
        val result: Int = growthRateService.calculateGrowthRate("unknown", 10)

        assertThat(result).isZero()
    }

    private fun validateGrowthRate(
        type: String,
        resourceName: String,
    ) {
        // 读取JSON文件
        val growthRateValidateResults: List<GrowthRateValidateResult> =
            jsonMapper.readValue(
                ClassPathResource(resourceName).inputStream,
                object : TypeReference<List<GrowthRateValidateResult>>() {},
            )
        // 对每个等级验证
        for (growthRateValidateResult in growthRateValidateResults) {
            val level = growthRateValidateResult.level
            val expectedExperience = growthRateValidateResult.experience
            val result: Int = growthRateService.calculateGrowthRate(type, level)
            assertThat(result).isEqualTo(expectedExperience)
        }
    }

    private data class GrowthRateValidateResult(
        val experience: Int,
        val level: Int,
    )
}
