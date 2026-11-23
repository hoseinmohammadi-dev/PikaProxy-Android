package io.github.ulysseszh.sunproxy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProxyEndpointTest {
	@Test fun acceptsBothCompanyServers() {
		assertEquals(ProxyEndpoint.DEFAULT, ProxyEndpoint.parse("192.168.1.244", "10809"))
		assertEquals(ProxyEndpoint.LEGACY, ProxyEndpoint.parse("192.168.1.65", "2080"))
	}

	@Test fun normalizesPersianAndArabicDigits() {
		assertEquals(ProxyEndpoint.DEFAULT, ProxyEndpoint.parse(" ۱۹۲.۱۶۸.۱.۲۴۴ ", " ١٠٨٠٩ "))
	}

	@Test fun rejectsUrlsHostnamesAndMalformedAddresses() {
		listOf("", "192.168.1", "192.168.1.256", "192.168.01.65", "http://192.168.1.244",
			"192.168.1.244:10809", "proxy.local", "::1", "0.0.0.0", "127.0.0.1", "224.0.0.1",
			"999999999999.1.1.1", "192.168.1.-1").forEach { assertNull(it, ProxyEndpoint.parse(it, "10809")) }
	}

	@Test fun enforcesPortBounds() {
		listOf("", "0", "65536", "-1", "+80", "1.5", "999999999999").forEach {
			assertNull(it, ProxyEndpoint.parse("192.168.1.244", it))
		}
		assertEquals(1, ProxyEndpoint.parse("192.168.1.244", "1")?.port)
		assertEquals(65535, ProxyEndpoint.parse("192.168.1.244", "65535")?.port)
	}
}
