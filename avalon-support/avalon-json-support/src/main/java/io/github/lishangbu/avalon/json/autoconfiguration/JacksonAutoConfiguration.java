package io.github.lishangbu.avalon.json.autoconfiguration;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.github.lishangbu.avalon.json.module.Jdk8JavaTimeModule;
import io.github.lishangbu.avalon.json.util.JsonUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * jackson自动配置
 *
 * @author lishangbu
 * @since 2022/12/22
 */
@AutoConfiguration(
    before = org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration.class)
public class JacksonAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public ObjectMapper objectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    // 配置ObjectMapper：设置日期格式为毫秒值
    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
    // 排序key
    objectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    // 忽略空bean转json错误
    objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    // 忽略在json字符串中存在，在java类中不存在字段，防止错误。
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    // 单引号处理
    objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
    objectMapper.configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true);

    // 处理jdk8日期时间模块
    objectMapper.registerModule(new Jdk8JavaTimeModule());

    // 处理长整型数据,序列换成json时,将所有的long变成string,避免js中精度丢失的问题
    SimpleModule longSerializerModule = new SimpleModule();
    longSerializerModule.addSerializer(Long.class, ToStringSerializer.instance);
    longSerializerModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
    objectMapper.registerModule(longSerializerModule);

    return objectMapper;
  }

  @Bean
  @ConditionalOnMissingBean
  public JsonUtils jsonUtils() {
    return new JsonUtils();
  }
}
