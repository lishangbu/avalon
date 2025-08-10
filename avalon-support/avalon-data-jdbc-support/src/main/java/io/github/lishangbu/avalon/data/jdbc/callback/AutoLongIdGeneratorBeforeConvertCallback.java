package io.github.lishangbu.avalon.data.jdbc.callback;

import io.github.lishangbu.avalon.data.jdbc.id.AutoLongIdGenerator;
import io.github.lishangbu.avalon.keygen.FlexKeyGenerator;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.relational.core.mapping.event.BeforeConvertCallback;
import org.springframework.stereotype.Component;

/**
 * 主键自动填充
 *
 * @author lishangbu
 * @since 2025/8/10
 */
@Component
public class AutoLongIdGeneratorBeforeConvertCallback
    implements BeforeConvertCallback<AutoLongIdGenerator> {

  private static Integer WORKER_ID = 0;

  private static final Logger log =
      LoggerFactory.getLogger(AutoLongIdGeneratorBeforeConvertCallback.class);

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
          int result = (int) (macAddressValue % 100);
          log.debug("本机MAC地址对应的数字:{}", result);
          WORKER_ID = result;
          break; // 只处理一个网络接口
        }
      }
    } catch (SocketException e) {
      log.error("读取本机MAC地址异常", e);
    }
  }

  @Override
  public AutoLongIdGenerator onBeforeConvert(AutoLongIdGenerator aggregate) {
    if (aggregate.getId() == null) {
      System.out.println("AutoLongIdGeneratorBeforeConvertCallback onBeforeConvert:" + WORKER_ID);
      aggregate.setId(FlexKeyGenerator.getInstance(WORKER_ID).generate());
    }
    return aggregate;
  }
}
