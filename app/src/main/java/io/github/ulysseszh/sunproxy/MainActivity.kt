package io.github.ulysseszh.sunproxy

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.net.VpnService
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.color.DynamicColors

class MainActivity : AppCompatActivity() {
	private lateinit var toggleButton: Button
	private lateinit var statusText: TextView
	private lateinit var statusDot: View
	private lateinit var actionHint: TextView
	private lateinit var pulseRing: View
	private lateinit var logoView: ImageView
	private var pulseAnimator: AnimatorSet? = null
	private var lastConnected: Boolean? = null
	private val statusHandler = Handler(Looper.getMainLooper())
	private var service: MyVpnService? = null

	private val vpnPermissionLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) { result ->
		if (result.resultCode == RESULT_OK) {
			MyVpnService.start(this)
		}
	}

	private val serviceConnection = object : ServiceConnection {
		override fun onServiceConnected(name: ComponentName, binder: IBinder) {
			service = (binder as MyVpnService.ServiceBinder).getService()
			updateStatus()
		}

		override fun onServiceDisconnected(name: ComponentName) {
			service = null
			updateStatus()
		}
	}

	private val statusRunnable = object : Runnable {
		override fun run() {
			updateStatus()
			statusHandler.postDelayed(this, 500)
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		enableEdgeToEdge()
		super.onCreate(savedInstanceState)
		DynamicColors.applyToActivityIfAvailable(this)
		setContentView(R.layout.activity_main)

		toggleButton = findViewById(R.id.toggle_button)
		statusText = findViewById(R.id.status_text)
		statusDot = findViewById(R.id.status_dot)
		actionHint = findViewById(R.id.action_hint)
		pulseRing = findViewById(R.id.pulse_ring)
		logoView = findViewById(R.id.logo_view)

		toggleButton.setOnClickListener {
			if (service?.isRunning() == true) {
				MyVpnService.stop(this)
			} else {
				val permissionIntent = VpnService.prepare(this)
				if (permissionIntent == null) {
					MyVpnService.start(this)
				} else {
					vpnPermissionLauncher.launch(permissionIntent)
				}
			}
		}
	}

	override fun onStart() {
		super.onStart()
		bindService(Intent(this, MyVpnService::class.java), serviceConnection, BIND_AUTO_CREATE)
		statusHandler.post(statusRunnable)
	}

	override fun onStop() {
		statusHandler.removeCallbacks(statusRunnable)
		if (service != null) {
			unbindService(serviceConnection)
			service = null
		}
		super.onStop()
	}

	private fun updateStatus() {
		val connected = service?.isRunning() == true
		statusText.setText(if (connected) R.string.status_on else R.string.status_off)
		actionHint.setText(if (connected) R.string.tap_to_disconnect else R.string.tap_to_connect)
		toggleButton.contentDescription = getString(if (connected) R.string.disconnect else R.string.connect)
		val statusColor = ContextCompat.getColor(this, if (connected) R.color.status_on else R.color.status_off)
		(statusDot.background.mutate() as GradientDrawable).setColor(statusColor)
		toggleButton.backgroundTintList = ColorStateList.valueOf(
			ContextCompat.getColor(this, if (connected) R.color.brand_blue else R.color.power_off)
		)
		if (lastConnected != connected) {
			animateStateChange(connected)
			lastConnected = connected
		}
	}

	private fun animateStateChange(connected: Boolean) {
		pulseAnimator?.cancel()
		toggleButton.animate().cancel()
		toggleButton.scaleX = 0.88f
		toggleButton.scaleY = 0.88f
		toggleButton.animate().scaleX(1f).scaleY(1f).setDuration(320).start()
		logoView.animate().rotationBy(connected.let { if (it) 6f else -6f }).setDuration(320).withEndAction {
			logoView.animate().rotation(0f).setDuration(220).start()
		}.start()

		if (connected) {
			val scaleX = ObjectAnimator.ofFloat(pulseRing, View.SCALE_X, 0.82f, 1.24f)
			val scaleY = ObjectAnimator.ofFloat(pulseRing, View.SCALE_Y, 0.82f, 1.24f)
			val alpha = ObjectAnimator.ofFloat(pulseRing, View.ALPHA, 0.75f, 0f)
			pulseAnimator = AnimatorSet().apply {
				playTogether(scaleX, scaleY, alpha)
				duration = 1500
				startDelay = 100
				addListener(object : android.animation.AnimatorListenerAdapter() {
					override fun onAnimationEnd(animation: android.animation.Animator) {
						if (service?.isRunning() == true) start()
					}
				})
				start()
			}
		} else {
			pulseRing.alpha = 0f
			pulseRing.scaleX = 1f
			pulseRing.scaleY = 1f
		}
	}
}
