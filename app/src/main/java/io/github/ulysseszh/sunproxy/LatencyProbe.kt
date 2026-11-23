package io.github.ulysseszh.sunproxy

import android.content.Context
import libv2ray.Libv2ray
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL

object LatencyProbe {
	/** HTTPS path delay, not ICMP and not bandwidth. Never falls back to direct. */
	fun measure(context: Context, profile: ServerProfile): Long {
		val target = "https://www.google.com/generate_204"
		if (profile.personal) {
			CoreEngine.initialize(context)
			return Libv2ray.measureOutboundDelay(PersonalConfig.build(profile.link), target).also { require(it >= 0) }
		}
		val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(profile.host, profile.port))
		val connection = URL(target).openConnection(proxy) as HttpURLConnection
		connection.connectTimeout=5000;connection.readTimeout=5000;connection.useCaches=false
		connection.instanceFollowRedirects=false
		connection.setRequestProperty("Connection","close")
		val start=System.nanoTime()
		try {
			require(connection.responseCode==204)
			return ((System.nanoTime()-start)/1_000_000).coerceAtLeast(1)
		} finally { connection.disconnect() }
	}
}
