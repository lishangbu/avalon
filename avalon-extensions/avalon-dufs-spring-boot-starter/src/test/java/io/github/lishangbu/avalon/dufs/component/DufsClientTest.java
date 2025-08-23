package io.github.lishangbu.avalon.dufs.component;

import io.github.lishangbu.avalon.dufs.autoconfiguration.DufsAutoConfiguration;
import jakarta.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

/**
 * @author lishangbu
 * @since 2025/8/11
 */
@SpringBootTest(classes = DufsAutoConfiguration.class)
class DufsClientTest {

  @Resource private DufsClient dufsClient;

  @Test
  void upload() throws IOException {

    dufsClient.upload(
        new MockMultipartFile("test5.txt", new ByteArrayInputStream("APTX-4869!".getBytes())),
        "test112");
  }

  @Test
  void mkdir() {
    dufsClient.mkdir("test12");
  }

  @Test
  void delete() {
    dufsClient.delete("test12");
  }
}
