package io.github.ulysseszh.sunproxy

data class ProxyEndpoint(val host: String, val port: Int) {
	companion object {
		val DEFAULT = ProxyEndpoint("192.168.1.244", 10809)
		val LEGACY = ProxyEndpoint("192.168.1.65", 2080)

		private fun normalize(value: String): String = value.trim().map {
			if (it in '۰'..'۹') '0' + (it - '۰')
			else if (it in '٠'..'٩') '0' + (it - '٠') else it
		}.joinToString("")

		fun parse(host: String, port: String): ProxyEndpoint? {
			val address = normalize(host)
			val parts = address.split('.')
			if (parts.size != 4 || parts.any {
				it.isEmpty() || it.length > 3 || it.any { c -> c !in '0'..'9' } ||
				(it.length > 1 && it.startsWith('0')) || (it.toIntOrNull() ?: -1) !in 0..255
			}) return null
			// Unspecified, loopback, multicast and broadcast addresses are not proxy servers.
			if (parts[0].toInt() in listOf(0, 127) || parts[0].toInt() >= 224) return null
			val portText = normalize(port)
			if (portText.isEmpty() || portText.any { it !in '0'..'9' }) return null
			val number = portText.toIntOrNull() ?: return null
			if (number !in 1..65535) return null
			return ProxyEndpoint(address, number)
		}
	}
}
