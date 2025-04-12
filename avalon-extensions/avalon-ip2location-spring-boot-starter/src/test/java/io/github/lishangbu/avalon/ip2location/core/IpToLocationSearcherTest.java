package io.github.lishangbu.avalon.ip2location.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.github.lishangbu.avalon.ip2location.properties.IpToLocationProperties;
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
  void ipQuery() {
    IpResult ipResult = ipToLocationSearcher.ipQuery("39.189.23.43");
    assertEquals("Not_Supported", ipResult.getAddressType());
    assertEquals("Not_Supported", ipResult.getAreaCode());
    assertEquals("Not_Supported", ipResult.getAs());
    assertEquals("Not_Supported", ipResult.getAsn());
    assertEquals("Not_Supported", ipResult.getCategory());
    assertEquals("Ningbo", ipResult.getCity());
    assertEquals("CN", ipResult.getCountryShort());
    assertEquals("China", ipResult.getCountryLong());
    assertFalse(ipResult.isDelay());
    assertEquals("Not_Supported", ipResult.getDistrict());
    assertEquals("Not_Supported", ipResult.getDomain());
    assertEquals(0.0f, ipResult.getElevation());
    assertEquals("Not_Supported", ipResult.getIddCode());
    assertEquals("Not_Supported", ipResult.getIsp());
    assertEquals(29.87841f, ipResult.getLatitude());
    assertEquals(121.54977f, ipResult.getLongitude());
    assertEquals("Not_Supported", ipResult.getMobileBrand());
    assertEquals("Not_Supported", ipResult.getMnc());
    assertEquals("Not_Supported", ipResult.getNetSpeed());
    assertEquals("Zhejiang", ipResult.getRegion());
    assertEquals("OK", ipResult.getStatus());
    assertEquals("+08:00", ipResult.getTimezone());
    assertEquals("Not_Supported", ipResult.getUsageType());
    assertEquals("Not_Supported", ipResult.getWeatherStationCode());
    assertEquals("Not_Supported", ipResult.getWeatherStationName());
    assertEquals("330201", ipResult.getZipcode());
  }
}
