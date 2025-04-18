package io.github.lishangbu.avalon.ip2location.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.github.lishangbu.avalon.ip2location.exception.EmptyIpAddressException;
import io.github.lishangbu.avalon.ip2location.properties.IpToLocationProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author lishangbu
 * @since 2025/4/12
 */
@SpringBootTest(classes = {IpToLocationSearcher.class, IpToLocationProperties.class})
class IpToLocationSearcherTest {
  @Autowired private IpToLocationSearcher ipToLocationSearcher;

  @Test
  void testIpQuery() {
    IpResult ipResult = ipToLocationSearcher.ipQuery("39.189.23.43");
    assertEquals("Not_Supported", ipResult.addressType());
    assertEquals("Not_Supported", ipResult.areaCode());
    assertEquals("Not_Supported", ipResult.as());
    assertEquals("Not_Supported", ipResult.asn());
    assertEquals("Not_Supported", ipResult.category());
    assertEquals("Ningbo", ipResult.city());
    assertEquals("CN", ipResult.countryShort());
    assertEquals("China", ipResult.countryLong());
    assertFalse(ipResult.delay());
    assertEquals("Not_Supported", ipResult.district());
    assertEquals("Not_Supported", ipResult.domain());
    assertEquals(0.0f, ipResult.elevation());
    assertEquals("Not_Supported", ipResult.iddCode());
    assertEquals("Not_Supported", ipResult.isp());
    assertEquals(29.87841f, ipResult.latitude());
    assertEquals(121.54977f, ipResult.longitude());
    assertEquals("Not_Supported", ipResult.mobileBrand());
    assertEquals("Not_Supported", ipResult.mnc());
    assertEquals("Not_Supported", ipResult.netSpeed());
    assertEquals("Zhejiang", ipResult.region());
    assertEquals("OK", ipResult.status());
    assertEquals("+08:00", ipResult.timezone());
    assertEquals("Not_Supported", ipResult.usageType());
    assertEquals("Not_Supported", ipResult.weatherStationCode());
    assertEquals("Not_Supported", ipResult.weatherStationName());
    assertEquals("330201", ipResult.zipcode());
  }

  @Test
  void testIpQueryWithEmptyIp() {
    Assertions.assertThrows(EmptyIpAddressException.class, () -> ipToLocationSearcher.ipQuery(""));
  }
}
