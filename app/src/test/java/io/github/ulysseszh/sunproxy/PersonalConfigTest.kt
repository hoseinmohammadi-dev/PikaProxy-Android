package io.github.ulysseszh.sunproxy

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.Base64

class PersonalConfigTest {
	private val id="11111111-1111-4111-8111-111111111111"
	@Test fun vlessWebsocketTls() {
		val o=PersonalConfig.outbound("vless://$id@example.com:443?type=ws&security=tls&sni=cdn.example.com&host=ws.example.com&path=%2Fsocket#test")
		assertEquals("vless",o.getString("protocol"))
		val s=o.getJSONObject("streamSettings")
		assertEquals("/socket",s.getJSONObject("wsSettings").getString("path"))
		assertEquals("ws.example.com",s.getJSONObject("wsSettings").getString("host"))
		assertFalse(s.getJSONObject("tlsSettings").getBoolean("allowInsecure"))
	}
	@Test fun realityXhttpPreservesParameters() {
		val o=PersonalConfig.outbound("vless://$id@203.0.113.1:443?type=xhttp&security=reality&sni=example.com&pbk=public-key&sid=1234&mode=auto&path=%2Ftest&extra=%7B%22noGRPCHeader%22%3Atrue%7D")
		val s=o.getJSONObject("streamSettings")
		assertEquals("public-key",s.getJSONObject("realitySettings").getString("publicKey"))
		assertTrue(s.getJSONObject("xhttpSettings").getJSONObject("extra").getBoolean("noGRPCHeader"))
	}
	@Test fun trojanPasswordPreservesPlusAndPercentEncoding() {
		val o=PersonalConfig.outbound("trojan://secret%2Bword%3Avalue@example.com:443?security=tls&type=tcp")
		assertEquals("secret+word:value",o.getJSONObject("settings").getJSONArray("servers").getJSONObject(0).getString("password"))
	}
	@Test fun vmessBase64JsonWithNumericPort() {
		val v=JSONObject().put("add","example.com").put("port",443).put("id",id).put("aid",0)
			.put("net","ws").put("tls","tls").put("path","/vmess").put("sni","example.com")
		val o=PersonalConfig.outbound("vmess://"+Base64.getEncoder().withoutPadding().encodeToString(v.toString().toByteArray()))
		assertEquals("vmess",o.getString("protocol"))
		assertEquals(443,o.getJSONObject("settings").getJSONArray("vnext").getJSONObject(0).getInt("port"))
	}
	@Test fun generatedConfigListensOnlyOnLoopbackAndHasNoDirectFallback() {
		val c=JSONObject(PersonalConfig.build("vless://$id@example.com:443?security=tls&type=grpc&serviceName=test"))
		assertEquals("127.0.0.1",c.getJSONArray("inbounds").getJSONObject(0).getString("listen"))
		assertEquals(1,c.getJSONArray("outbounds").length())
		assertEquals("grpc",c.getJSONArray("outbounds").getJSONObject(0).getJSONObject("streamSettings").getString("network"))
	}
	@Test fun rejectsMalformedAndUnsupportedLinks() {
		listOf("", "https://example.com/sub/secret", "vless://bad@example.com:443", "trojan://x@example.com:70000",
			"vless://$id@example.com:443?type=kcp", "vmess://invalid-base64!", "vless://$id@example.com:443?security=reality",
			"trojan://x@example.com:443?allowInsecure=1", "vless://$id@example.com:443?headerType=http",
			"trojan://x@example.com:443\ntrojan://y@example.com:443").forEach { link ->
			assertTrue("Expected rejection",runCatching { PersonalConfig.outbound(link) }.isFailure)
		}
	}
	@Test fun nativeCoreAcceptsGeneratedConfigurations() {
		val executable=System.getenv("PIKA_XRAY_TEST_EXECUTABLE")
		org.junit.Assume.assumeTrue(executable!=null)
		val vmess=JSONObject().put("add","example.com").put("port",443).put("id",id)
			.put("net","ws").put("tls","tls").put("path","/socket")
		val links=listOf(
			"vless://$id@example.com:443?type=ws&security=tls&path=%2Fsocket",
			"trojan://test-password@example.com:443?security=tls&type=grpc&serviceName=test",
			"vless://$id@203.0.113.1:443?type=xhttp&security=reality&sni=example.com&pbk=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA&sid=1234",
			"vmess://"+Base64.getEncoder().encodeToString(vmess.toString().toByteArray()))
		for(link in links) {
			val config=java.io.File.createTempFile("pika-core-test-", ".json")
			try {
				config.writeText(PersonalConfig.build(link))
				val process=ProcessBuilder(executable!!,"run","-test","-config",config.absolutePath).redirectErrorStream(true).start()
				val output=process.inputStream.bufferedReader().readText()
				assertEquals(output,0,process.waitFor())
			} finally { config.delete() }
		}
	}
}
