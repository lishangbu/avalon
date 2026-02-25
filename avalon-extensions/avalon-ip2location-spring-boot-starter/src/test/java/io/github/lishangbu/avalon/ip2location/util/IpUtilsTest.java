package io.github.lishangbu.avalon.ip2location.util;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import java.net.UnknownHostException;
import org.junit.jupiter.api.Test;

/// 单元测试：IpUtils
///
/// 测试网络地址工具类的功能（IPv4/IPv6 判定与转换、CIDR 与十进制转换等）
///
/// @author lishangbu
/// @since 2025/4/12
class IpUtilsTest {

    @Test
    void isIpv4() {
        assertTrue(IpUtils.isIpv4("8.8.8.8"));
        assertFalse(IpUtils.isIpv4("2600:1f18:45b0:5b00:f5d8:4183:7710:ceec"));
    }

    @Test
    void isIpv6() {
        assertFalse(IpUtils.isIpv6("8.8.8.8"));
        assertTrue(IpUtils.isIpv6("2600:1f18:45b0:5b00:f5d8:4183:7710:ceec"));
    }

    @Test
    void ipv4ToDecimal() {
        assertEquals(BigInteger.valueOf(134744072), IpUtils.ipv4ToDecimal("8.8.8.8"));
        assertNull(IpUtils.ipv4ToDecimal("2600:1f18:45b0:5b00:f5d8:4183:7710:ceec"));
    }

    @Test
    void ipv6ToDecimal() {
        assertEquals(
                new BigInteger("50511294517568083089461819682126352108"),
                IpUtils.ipv6ToDecimal("2600:1f18:45b0:5b00:f5d8:4183:7710:ceec"));
        assertNull(IpUtils.ipv6ToDecimal("8.8.8.8"));
    }

    @Test
    void decimalToIpv4() throws UnknownHostException {
        assertEquals("8.8.8.8", IpUtils.decimalToIpv4(BigInteger.valueOf(134744072)));
    }

    @Test
    void decimalToIpv6() throws UnknownHostException {
        assertEquals(
                "2600:1f18:45b0:5b00:f5d8:4183:7710:ceec",
                IpUtils.decimalToIpv6(new BigInteger("50511294517568083089461819682126352108")));
    }

    @Test
    void compressIpv6() {
        assertEquals(
                "::35:0:ffff:0:0", IpUtils.compressIpv6("0000:0000:0000:0035:0000:FFFF:0000:0000"));
    }

    @Test
    void expandIpv6() {
        assertEquals(
                "0500:6001:00fe:0035:0000:ffff:0000:0000",
                IpUtils.expandIpv6("500:6001:FE:35:0:FFFF::"));
    }

    @Test
    void ipv4ToCidr() throws UnknownHostException {
        assertArrayEquals(
                new String[] {"10.0.0.0/13", "10.8.0.0/15", "10.10.0.0/23", "10.10.2.0/24"},
                IpUtils.ipv4ToCidr("10.0.0.0", "10.10.2.255").toArray());
    }

    @Test
    void ipv6ToCidr() throws UnknownHostException {
        assertArrayEquals(
                new String[] {
                    "2001:4860:4860::8888/125",
                    "2001:4860:4860::8890/124",
                    "2001:4860:4860::88a0/123",
                    "2001:4860:4860::88c0/122",
                    "2001:4860:4860::8900/120",
                    "2001:4860:4860::8a00/119",
                    "2001:4860:4860::8c00/118",
                    "2001:4860:4860::9000/116",
                    "2001:4860:4860::a000/115",
                    "2001:4860:4860::c000/114",
                    "2001:4860:4860::1:0/112",
                    "2001:4860:4860::2:0/111",
                    "2001:4860:4860::4:0/110",
                    "2001:4860:4860::8:0/109",
                    "2001:4860:4860::10:0/108",
                    "2001:4860:4860::20:0/107",
                    "2001:4860:4860::40:0/106",
                    "2001:4860:4860::80:0/105",
                    "2001:4860:4860::100:0/104",
                    "2001:4860:4860::200:0/103",
                    "2001:4860:4860::400:0/102",
                    "2001:4860:4860::800:0/101",
                    "2001:4860:4860::1000:0/100",
                    "2001:4860:4860::2000:0/99",
                    "2001:4860:4860::4000:0/98",
                    "2001:4860:4860::8000:0/97",
                    "2001:4860:4860::1:0:0/96",
                    "2001:4860:4860::2:0:0/95",
                    "2001:4860:4860::4:0:0/94",
                    "2001:4860:4860::8:0:0/93",
                    "2001:4860:4860::10:0:0/92",
                    "2001:4860:4860::20:0:0/91",
                    "2001:4860:4860::40:0:0/90",
                    "2001:4860:4860::80:0:0/89",
                    "2001:4860:4860::100:0:0/88",
                    "2001:4860:4860::200:0:0/87",
                    "2001:4860:4860::400:0:0/86",
                    "2001:4860:4860::800:0:0/85",
                    "2001:4860:4860::1000:0:0/84",
                    "2001:4860:4860::2000:0:0/83",
                    "2001:4860:4860::4000:0:0/82",
                    "2001:4860:4860::8000:0:0/81",
                    "2001:4860:4860:0:1::/80",
                    "2001:4860:4860:0:2::/79",
                    "2001:4860:4860:0:4::/78",
                    "2001:4860:4860:0:8::/77",
                    "2001:4860:4860:0:10::/76",
                    "2001:4860:4860:0:20::/75",
                    "2001:4860:4860:0:40::/74",
                    "2001:4860:4860:0:80::/73",
                    "2001:4860:4860:0:100::/72",
                    "2001:4860:4860:0:200::/71",
                    "2001:4860:4860:0:400::/70",
                    "2001:4860:4860:0:800::/69",
                    "2001:4860:4860:0:1000::/68",
                    "2001:4860:4860:0:2000::/67",
                    "2001:4860:4860:0:4000::/66",
                    "2001:4860:4860:0:8000::/66",
                    "2001:4860:4860:0:c000::/67",
                    "2001:4860:4860:0:e000::/69",
                    "2001:4860:4860:0:e800::/70",
                    "2001:4860:4860:0:ec00::/71",
                    "2001:4860:4860:0:ee00::/73",
                    "2001:4860:4860:0:ee80::/74",
                    "2001:4860:4860:0:eec0::/75",
                    "2001:4860:4860:0:eee0::/77",
                    "2001:4860:4860:0:eee8::/78",
                    "2001:4860:4860:0:eeec::/79",
                    "2001:4860:4860:0:eeee::/80"
                },
                IpUtils.ipv6ToCidr(
                                "2001:4860:4860:0000:0000:0000:0000:8888",
                                "2001:4860:4860:0000:eeee:ffff:ffff:ffff")
                        .toArray());
    }

    @Test
    void cidrToIpv4() throws UnknownHostException {
        assertArrayEquals(
                new String[] {"10.112.0.0", "10.127.255.255"},
                IpUtils.cidrToIpv4("10.123.80.0/12"));
    }

    @Test
    void cidrToIpv6() throws UnknownHostException {
        assertArrayEquals(
                new String[] {
                    "2002:1234:0000:0000:0000:0000:0000:0000",
                    "2002:1234:0000:0003:ffff:ffff:ffff:ffff"
                },
                IpUtils.cidrToIpv6("2002:1234::abcd:ffff:c0a8:101/62"));
    }
}
