package io.github.lishangbu.avalon.ip2location.core

import io.github.lishangbu.avalon.ip2location.exception.EmptyIpAddressException
import io.github.lishangbu.avalon.ip2location.exception.InvalidIpAddressException
import io.github.lishangbu.avalon.ip2location.exception.IpToLocationException
import io.github.lishangbu.avalon.ip2location.exception.Ipv6NotSupportException
import io.github.lishangbu.avalon.ip2location.exception.MissingFileException
import io.github.lishangbu.avalon.ip2location.properties.IpToLocationProperties
import net.renfei.ip2location.IP2Location
import net.renfei.ip2location.IPResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.core.io.ResourceLoader
import org.springframework.test.util.ReflectionTestUtils
import java.io.IOException
import java.util.stream.Stream
import kotlin.reflect.KClass

class IpToLocationSearcherUnitTest {
    private val properties = IpToLocationProperties()
    private val resourceLoader = mock(ResourceLoader::class.java)

    @Test
    fun returnsNullWhenEngineIsNotInitialized() {
        val searcher = IpToLocationSearcher(properties, resourceLoader)

        assertNull(searcher.ipQuery("1.1.1.1"))
    }

    @Test
    fun mapsSuccessfulIpLookup() {
        val searcher = IpToLocationSearcher(properties, resourceLoader)
        val engine = mock(IP2Location::class.java)
        val originResult = successResult()
        ReflectionTestUtils.setField(searcher, "loc", engine)
        `when`(engine.IPQuery("1.1.1.1")).thenReturn(originResult)

        val result = requireNotNull(searcher.ipQuery("1.1.1.1"))

        assertEquals("A", result.addressType)
        assertEquals("Ningbo", result.city)
        assertEquals("China", result.countryLong)
        assertEquals("OK", result.status)
        assertEquals("330201", result.zipcode)
    }

    @ParameterizedTest
    @MethodSource("statusCases")
    fun throwsExpectedExceptionForKnownStatuses(
        status: String,
        exceptionType: KClass<out RuntimeException>,
    ) {
        val searcher = IpToLocationSearcher(properties, resourceLoader)
        val engine = mock(IP2Location::class.java)
        val originResult = statusResult(status)
        ReflectionTestUtils.setField(searcher, "loc", engine)
        `when`(engine.IPQuery("1.1.1.1")).thenReturn(originResult)

        assertThrows(exceptionType.java) {
            searcher.ipQuery("1.1.1.1")
        }
    }

    @Test
    fun wrapsIoExceptionAsMissingFileException() {
        val searcher = IpToLocationSearcher(properties, resourceLoader)
        val engine = mock(IP2Location::class.java)
        ReflectionTestUtils.setField(searcher, "loc", engine)
        `when`(engine.IPQuery("1.1.1.1")).thenThrow(IOException("broken db"))

        assertThrows(MissingFileException::class.java) {
            searcher.ipQuery("1.1.1.1")
        }
    }

    @Test
    fun destroyClosesEngine() {
        val searcher = IpToLocationSearcher(properties, resourceLoader)
        val engine = mock(IP2Location::class.java)
        ReflectionTestUtils.setField(searcher, "loc", engine)

        searcher.destroy()

        verify(engine).Close()
    }

    companion object {
        @JvmStatic
        fun statusCases(): Stream<Arguments> =
            Stream.of(
                Arguments.of("EMPTY_IP_ADDRESS", EmptyIpAddressException::class),
                Arguments.of("INVALID_IP_ADDRESS", InvalidIpAddressException::class),
                Arguments.of("MISSING_FILE", MissingFileException::class),
                Arguments.of("IPV6_NOT_SUPPORTED", Ipv6NotSupportException::class),
                Arguments.of("SOMETHING_ELSE", IpToLocationException::class),
            )

        private fun statusResult(status: String): IPResult =
            mock(IPResult::class.java).also {
                `when`(it.status).thenReturn(status)
            }

        private fun successResult(): IPResult =
            mock(IPResult::class.java).also {
                `when`(it.status).thenReturn("OK")
                `when`(it.getAddressType()).thenReturn("A")
                `when`(it.getAreaCode()).thenReturn("574")
                `when`(it.getAS()).thenReturn("AS-EXAMPLE")
                `when`(it.getASN()).thenReturn("64512")
                `when`(it.getCategory()).thenReturn("RESIDENTIAL")
                `when`(it.getCity()).thenReturn("Ningbo")
                `when`(it.getCountryShort()).thenReturn("CN")
                `when`(it.getCountryLong()).thenReturn("China")
                `when`(it.getDelay()).thenReturn(false)
                `when`(it.getDistrict()).thenReturn("Haishu")
                `when`(it.getDomain()).thenReturn("example.com")
                `when`(it.getElevation()).thenReturn(1.2f)
                `when`(it.getIDDCode()).thenReturn("86")
                `when`(it.getISP()).thenReturn("ISP")
                `when`(it.getLatitude()).thenReturn(29.86f)
                `when`(it.getLongitude()).thenReturn(121.62f)
                `when`(it.getMobileBrand()).thenReturn("Brand")
                `when`(it.getMCC()).thenReturn("460")
                `when`(it.getMNC()).thenReturn("00")
                `when`(it.getNetSpeed()).thenReturn("DSL")
                `when`(it.getRegion()).thenReturn("Zhejiang")
                `when`(it.getTimeZone()).thenReturn("+08:00")
                `when`(it.getUsageType()).thenReturn("COM")
                `when`(it.getVersion()).thenReturn("IPv4")
                `when`(it.getWeatherStationCode()).thenReturn("CHXX")
                `when`(it.getWeatherStationName()).thenReturn("Ningbo")
                `when`(it.getZipCode()).thenReturn("330201")
            }
    }
}
