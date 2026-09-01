package eu.faircode.netguard

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.os.Process
import android.util.Log
import androidx.annotation.RequiresApi
import io.github.ulysseszh.sunproxy.MyVpnService
import java.net.InetSocketAddress
import java.net.InetAddress

// This class acts as a between-layer of JNI and Kotlin.
// The class name and method names are taken from NetGuard.
@Suppress("unused")
class ServiceSinkhole(val service: MyVpnService) {

	companion object {
		const val TAG = "NetGuard service"
	}

	external fun jni_init(sdk: Int): Long
	external fun jni_start(context: Long, loglevel: Int)
	external fun jni_run(context: Long, tun: Int, fwd53: Boolean, rcode: Int)
	external fun jni_stop(context: Long)
	external fun jni_clear(context: Long)
	external fun jni_get_mtu(): Int
	external fun jni_get_stats(context: Long): IntArray
	external fun jni_pcap(name: String?, record_size: Int, file_size: Int)
	external fun jni_socks5(addr: String?, port: Int, username: String?, password: String?)
	external fun jni_done(context: Long)

	private fun nativeExit(reason: String) {
		Log.w(TAG, "Native exit reason=$reason")
	}

	private fun nativeError(error: Int, message: String) {
		Log.w(TAG, "Native error $error: $message")
	}

	private fun logPacket(packet: Packet) {
		Log.d(TAG, "Packet: $packet")
	}

	private fun dnsResolved(rr: ResourceRecord) {
		Log.d(TAG, "DNS resolved: $rr")
	}

	private fun isDomainBlocked(name: String): Boolean {
		return false
	}

	@RequiresApi(Build.VERSION_CODES.Q)
	private fun getUidQ(version: Int, protocol: Int, saddr: String, sport: Int, daddr: String, dport: Int): Int {
		if (protocol != 6 /* TCP */ && protocol != 17 /* UDP */) return Process.INVALID_UID

		val cm = service.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
		if (cm == null) return Process.INVALID_UID

		val local = InetSocketAddress(saddr, sport)
		val remote = InetSocketAddress(daddr, dport)

		Log.i(TAG, "Get uid local=$local remote=$remote")
		val uid = cm.getConnectionOwnerUid(protocol, local, remote)
		Log.i(TAG, "Get uid=$uid")
		return uid
	}

	private fun isAddressAllowed(packet: Packet): Allowed? {
		packet.allowed = true
		if (packet.uid == Process.myUid()) {
			return Allowed()
		}
		val protocol = when (packet.protocol) {
			1, 58 -> "icmp"
			6 -> "tcp"
			17 -> "udp"
			else -> ""
		}

		// Same intent as the Windows <local> bypass: private and local traffic
		// remains on the company network.
		if (isLocalAddress(packet.daddr)) {
			return Allowed()
		}

		return when {
			// Public TCP is wrapped in HTTP CONNECT by the native engine.
			protocol == "tcp" -> Allowed(MyVpnService.PROXY_HOST, MyVpnService.PROXY_PORT)
			// HTTP proxies cannot carry UDP. DNS stays usable; other public UDP is
			// blocked so browsers fall back from QUIC to proxied TCP.
			protocol == "udp" && packet.dport == 53 -> Allowed()
			else -> null
		}
	}

	private fun isLocalAddress(address: String): Boolean = try {
		InetAddress.getByName(address).let {
			it.isAnyLocalAddress || it.isLoopbackAddress || it.isLinkLocalAddress ||
				it.isSiteLocalAddress || it.isMulticastAddress || address == MyVpnService.PROXY_HOST
		}
	} catch (_: Exception) {
		false
	}

	private fun accountUsage(usage: Usage) {
		Log.i(TAG, "Usage: $usage")
	}

	/* Methods added by UlyssesZhan starting from here */

	private fun addressFromHosts(host: String, version: Int): String? {
		return service.addressFromHosts(host, version)
	}
}
