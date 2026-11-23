package io.github.ulysseszh.sunproxy

import android.content.Context
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.Libv2ray

object CoreEngine {
	private var initialized = false
	@Synchronized fun initialize(context: Context) {
		if (!initialized) {
			go.Seq.setContext(context.applicationContext)
			Libv2ray.initCoreEnv(context.filesDir.absolutePath, "")
			initialized = true
		}
	}
	fun start(context: Context, link: String): CoreController {
		initialize(context)
		val core = Libv2ray.newCoreController(object : CoreCallbackHandler {
			override fun startup() = 0L
			override fun shutdown() = 0L
			override fun onEmitStatus(code: Long, message: String?) = 0L
		})
		try { core.startLoop(PersonalConfig.build(link), 0) }
		catch (error: Exception) { runCatching { core.stopLoop() }; throw error }
		return core
	}
}
