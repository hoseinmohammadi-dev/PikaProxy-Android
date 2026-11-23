package io.github.ulysseszh.sunproxy

import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.net.URLDecoder
import java.io.ByteArrayOutputStream
import java.util.UUID

/** Individual share links only. No remote subscription or arbitrary executable config. */
object PersonalConfig {
	const val LOCAL_PORT = 18090
	private fun decode(s: String) = URLDecoder.decode(s.replace("+", "%2B"), "UTF-8")
	private fun base64(s: String): String {
		val value = s.trim().replace('-', '+').replace('_', '/')
		val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
		val output = ByteArrayOutputStream()
		var buffer = 0; var bits = 0
		for (c in value.trimEnd('=')) {
			val n = alphabet.indexOf(c);require(n >= 0)
			buffer = (buffer shl 6) or n;bits += 6
			if (bits >= 8) { bits -= 8;output.write((buffer shr bits) and 255) }
		}
		return output.toString("UTF-8")
	}

	fun outbound(link: String): JSONObject {
		require(link.length <= 32768 && !link.contains('\n') && !link.contains('\r'))
		val uri = URI(link.trim())
		val protocol = uri.scheme?.lowercase()
		require(protocol in setOf("vless", "vmess", "trojan"))
		val q = mutableMapOf<String, String>()
		var host: String
		var port: Int
		var credential: String
		if (protocol == "vmess") {
			val v = JSONObject(base64(link.trim().substringAfter("://")))
			host = v.getString("add"); port = v.get("port").toString().toInt(); credential = v.getString("id")
			q["type"] = v.optString("net", "tcp")
			q["security"] = if (v.optString("tls") == "tls") "tls" else "none"
			for (key in listOf("host", "path", "sni", "alpn", "fp", "scy")) q[key] = v.optString(key)
			q["aid"] = v.optString("aid", "0")
		} else {
			host = uri.host ?: throw IllegalArgumentException()
			port = uri.port
			credential = decode(uri.rawUserInfo ?: throw IllegalArgumentException())
			uri.rawQuery?.split('&')?.filter { it.isNotEmpty() }?.forEach {
				q[decode(it.substringBefore('='))] = decode(it.substringAfter('=', ""))
			}
		}
		require(host.isNotBlank() && port in 1..65535 && credential.isNotBlank())
		val user = JSONObject()
		if (protocol != "trojan") {
			UUID.fromString(credential)
			user.put("id", credential)
			if (protocol == "vless") {
				val encryption = q["encryption"] ?: "none"
				require(encryption == "none")
				user.put("encryption", encryption)
				q["flow"]?.takeIf { it.isNotBlank() }?.let { user.put("flow", it) }
			} else {
				user.put("security", q["scy"].orEmpty().ifBlank { "auto" })
				user.put("alterId", q["aid"]?.toIntOrNull() ?: 0)
			}
		}
		val settings = if (protocol == "trojan") JSONObject().put("servers", JSONArray().put(
			JSONObject().put("address", host).put("port", port).put("password", credential)))
		else JSONObject().put("vnext", JSONArray().put(JSONObject().put("address", host).put("port", port)
			.put("users", JSONArray().put(user))))
		val network = q["type"].orEmpty().ifBlank { "tcp" }
		require(network in setOf("tcp", "raw", "ws", "grpc", "xhttp", "httpupgrade"))
		val security = q["security"] ?: if (protocol == "trojan") "tls" else "none"
		require(security in setOf("none", "tls", "reality"))
		// Do not silently weaken certificate verification on imported links.
		require(q["allowInsecure"] !in setOf("1", "true") && q["insecure"] !in setOf("1", "true"))
		val stream = JSONObject().put("network", network).put("security", security)
		val path = q["path"].orEmpty().ifBlank { "/" }
		when (network) {
			"ws", "httpupgrade" -> stream.put(if (network == "ws") "wsSettings" else "httpupgradeSettings",
				JSONObject().put("path", path).put("host", q["host"].orEmpty().ifBlank { host }))
			"grpc" -> stream.put("grpcSettings", JSONObject().put("serviceName", q["serviceName"] ?: q["path"].orEmpty())
				.put("multiMode", q["mode"] == "multi"))
			"xhttp" -> {
				val x = JSONObject().put("path", path).put("host", q["host"].orEmpty()).put("mode", q["mode"] ?: "auto")
				q["extra"]?.let { x.put("extra", JSONObject(it)) }
				stream.put("xhttpSettings", x)
			}
			"tcp", "raw" -> require(q["headerType"].orEmpty() in setOf("", "none"))
		}
		if (security == "tls") {
			val tls = JSONObject().put("serverName", q["sni"].orEmpty().ifBlank { host }).put("allowInsecure", false)
			tls.put("fingerprint", q["fp"].orEmpty().ifBlank { "chrome" })
			q["alpn"]?.takeIf { it.isNotBlank() }?.let { tls.put("alpn", JSONArray(it.split(','))) }
			q["pinSHA256"]?.takeIf { it.isNotBlank() }?.let { tls.put("pinnedPeerCertificateChainSha256", JSONArray(it.split(','))) }
			stream.put("tlsSettings", tls)
		} else if (security == "reality") {
			require(!q["pbk"].isNullOrBlank() && !q["sni"].isNullOrBlank())
			val r = JSONObject().put("serverName", q["sni"]).put("publicKey", q["pbk"])
				.put("fingerprint", q["fp"].orEmpty().ifBlank { "chrome" }).put("shortId", q["sid"].orEmpty())
				.put("spiderX", q["spx"] ?: "/")
			q["pqv"]?.takeIf { it.isNotBlank() }?.let { r.put("mldsa65Verify", it) }
			stream.put("realitySettings", r)
		}
		return JSONObject().put("tag", "personal").put("protocol", protocol).put("settings", settings).put("streamSettings", stream)
	}

	fun build(link: String): String = JSONObject()
		.put("log", JSONObject().put("loglevel", "none"))
		.put("inbounds", JSONArray().put(JSONObject().put("tag", "local-http").put("listen", "127.0.0.1")
			.put("port", LOCAL_PORT).put("protocol", "http").put("settings", JSONObject())))
		.put("outbounds", JSONArray().put(outbound(link))).toString()
}
