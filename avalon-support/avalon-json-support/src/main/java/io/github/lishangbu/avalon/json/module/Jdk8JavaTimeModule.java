package io.github.lishangbu.avalon.json.module;

import com.fasterxml.jackson.core.json.PackageVersion;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * JDK8 时间序列化
 *
 * @author lishangbu
 * @since 2025/4/8
 */
public class Jdk8JavaTimeModule extends SimpleModule {

  private static final String NORM_DATETIME_FORMATTER = "yyyy-MM-dd HH:mm:ss";

  public Jdk8JavaTimeModule() {
    super(PackageVersion.VERSION);

    // ======================= 时间序列化规则 ===============================
    // yyyy-MM-dd HH:mm:ss
    this.addSerializer(
        LocalDateTime.class,
        new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(NORM_DATETIME_FORMATTER)));
    // yyyy-MM-dd
    this.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ISO_LOCAL_DATE));
    // HH:mm:ss
    this.addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ISO_LOCAL_TIME));
    // Instant 类型序列化
    this.addSerializer(Instant.class, InstantSerializer.INSTANCE);

    // ======================= 时间反序列化规则 ==============================
    // yyyy-MM-dd HH:mm:ss
    this.addDeserializer(
        LocalDateTime.class,
        new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(NORM_DATETIME_FORMATTER)));
    // yyyy-MM-dd
    this.addDeserializer(
        LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ISO_LOCAL_DATE));
    // HH:mm:ss
    this.addDeserializer(
        LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ISO_LOCAL_TIME));
    // Instant 反序列化
    this.addDeserializer(Instant.class, InstantDeserializer.INSTANT);
  }
}
