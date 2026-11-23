package io.github.ulysseszh.sunproxy

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ServerProfile(val id: String, val name: String, val host: String = "", val port: Int = 0, val link: String = "") {
	val personal get() = link.isNotEmpty()
	val summary get() = if (personal) link.substringBefore("://").uppercase() else "$host:$port"
	fun endpoint() = if (personal) ProxyEndpoint("127.0.0.1", PersonalConfig.LOCAL_PORT) else ProxyEndpoint(host, port)
}

object ServerProfiles {
	private const val PROFILES = "server_profiles_v1"
	private const val SELECTED = "selected_server_profile"
	@Volatile private var connectionActive = false
	fun load(context: Context): List<ServerProfile> {
		val prefs = PreferenceManager.getDefaultSharedPreferences(context)
		val stored = prefs.getString(PROFILES, null)
		if (stored != null) return try {
			val array = JSONArray(stored)
			(0 until array.length()).map { i -> array.getJSONObject(i).let {
				ServerProfile(it.getString("id"), it.getString("name"), it.optString("host"), it.optInt("port"), it.optString("link"))
			} }.ifEmpty { defaults(context) }
		} catch (_: Exception) { defaults(context) }
		return defaults(context)
	}
	private fun defaults(context: Context): List<ServerProfile> {
		val old = Pref.loadProxyEndpoint(context)
		val result = mutableListOf(
			ServerProfile("company-linux", "سرور جدید شرکت", ProxyEndpoint.DEFAULT.host, ProxyEndpoint.DEFAULT.port),
			ServerProfile("company-windows", "سرور قبلی شرکت", ProxyEndpoint.LEGACY.host, ProxyEndpoint.LEGACY.port))
		if (old != ProxyEndpoint.DEFAULT && old != ProxyEndpoint.LEGACY) result.add(0,ServerProfile("previous-custom", "سرور ذخیره‌شده", old.host, old.port))
		else if (old == ProxyEndpoint.LEGACY) result.reverse()
		return result
	}
	fun save(context: Context, profiles: List<ServerProfile>) {
		val array = JSONArray()
		profiles.forEach { array.put(JSONObject().put("id", it.id).put("name", it.name).put("host", it.host).put("port", it.port).put("link", it.link)) }
		PreferenceManager.getDefaultSharedPreferences(context).edit { putString(PROFILES, array.toString()) }
	}
	fun selected(context: Context): ServerProfile {
		val all = load(context)
		val id = PreferenceManager.getDefaultSharedPreferences(context).getString(SELECTED, null)
		return all.find { it.id == id } ?: all.first()
	}
	fun select(context: Context, id: String) {
		require(load(context).any { it.id == id })
		PreferenceManager.getDefaultSharedPreferences(context).edit { putString(SELECTED, id) }
	}
	fun newId() = UUID.randomUUID().toString()
	fun active(context: Context) = connectionActive
	fun markActive(context: Context, value: Boolean) { connectionActive = value }
}
