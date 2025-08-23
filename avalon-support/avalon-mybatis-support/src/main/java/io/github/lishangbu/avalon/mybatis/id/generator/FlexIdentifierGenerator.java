package io.github.lishangbu.avalon.mybatis.id.generator;

import io.github.lishangbu.avalon.keygen.FlexKeyGenerator;
import io.github.lishangbu.avalon.mybatis.id.IdType;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

/**
 * flex id 生成器
 *
 * @author lishangbu
 * @since 2025/8/20
 */
@Slf4j
public class FlexIdentifierGenerator implements IdentifierGenerator {

  private static int WORKER_ID = 0;

  static {
    try {
      // 获取本机的网络接口
      Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
      while (networkInterfaces.hasMoreElements()) {
        NetworkInterface networkInterface = networkInterfaces.nextElement();

        // 获取MAC地址
        byte[] mac = networkInterface.getHardwareAddress();
        if (mac != null) {
          // 将MAC地址转换成一个数字
          long macAddressValue = 0;
          for (int i = 0; i < mac.length; i++) {
            macAddressValue = (macAddressValue << 8) + (mac[i] & 0xFF);
          }

          // 将数字转换成0-99之间的数字
          WORKER_ID = (int) (macAddressValue % 100);
          log.debug("本机MAC地址对应的数字:{}", WORKER_ID);
          break; // 只处理一个网络接口
        }
      }
    } catch (SocketException e) {
      log.error("读取本机MAC地址异常", e);
    }
  }

  @Override
  public Serializable nextId(Field field, Object entity) {
    Assert.isTrue(field.getType().isAssignableFrom(Long.class), "Flex主键策略对应主键属性类型必须为Long");
    return FlexKeyGenerator.getInstance(this.WORKER_ID).generate();
  }

  @Override
  public IdType getIdType() {
    return IdType.FLEX;
  }
}
