package io.github.ulysseszh.sunproxy

import android.content.ClipboardManager
import android.os.Bundle
import android.graphics.Color
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ServerSettingsActivity : AppCompatActivity() {
	private lateinit var list: LinearLayout
	private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
	private fun message(text: String) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()
	private fun canChange(): Boolean {
		if (ServerProfiles.active(this)) { message("ابتدا اتصال را از صفحهٔ اصلی خاموش کنید."); return false }
		return true
	}
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val scroll = ScrollView(this).apply { isFillViewport = true; setBackgroundColor(Color.rgb(252,253,255)) }
		list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20),dp(16),dp(20),dp(24)) }
		scroll.addView(list);setContentView(scroll)
		ViewCompat.setOnApplyWindowInsetsListener(scroll) { v, i ->
			val p=i.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
			v.setPadding(p.left,p.top,p.right,p.bottom);i
		}
	}
	override fun onResume() { super.onResume(); render() }
	private fun render() {
		list.removeAllViews()
		list.addView(ServerSettingsView.create(this, ServerProfiles.load(this), ServerProfiles.selected(this).id,
			onBack = { finish() },
			onSelect = { if(canChange()) ServerProfiles.select(this,it.id);render() },
			onEdit = { if(canChange()) editor(it,it.personal) },
			onDelete = { if(canChange()) remove(it) },
			onAdd = { if(canChange()) editor(null,it) }))
	}

	private fun remove(profile: ServerProfile) {
		if(ServerProfiles.load(this).size==1) { message("حداقل یک سرور باید باقی بماند.");return }
		MaterialAlertDialogBuilder(this).setTitle("حذف ${profile.name}؟").setNegativeButton("انصراف",null)
			.setPositiveButton("حذف") { _,_->
				if(canChange()) { ServerProfiles.save(this,ServerProfiles.load(this).filter { it.id!=profile.id });render() }
			}.show()
	}
	private fun editor(old: ServerProfile?, personal: Boolean) {
		if(old==null && ServerProfiles.load(this).size>=50) { message("حداکثر ۵۰ سرور قابل ذخیره است.");return }
		val fields=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setPadding(dp(24),dp(8),dp(24),0) }
		fun field(label: String, value: String, type: Int): EditText {
			fields.addView(TextView(this).apply { text=label })
			return EditText(this).apply {
				inputType=type;setText(value);minHeight=dp(48);textDirection=View.TEXT_DIRECTION_LTR
				importantForAutofill=View.IMPORTANT_FOR_AUTOFILL_NO;fields.addView(this)
			}
		}
		val name=field("نام سرور",old?.name.orEmpty(),InputType.TYPE_CLASS_TEXT)
		val host=if(!personal) field("آدرس IPv4",old?.host.orEmpty(),InputType.TYPE_CLASS_TEXT) else null
		val port=if(!personal) field("پورت HTTP",old?.port?.toString() ?: "10809",InputType.TYPE_CLASS_NUMBER) else null
		val link=if(personal) field("لینک vless:// یا vmess:// یا trojan://",old?.link.orEmpty(),InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE).apply { maxLines=5 } else null
		if(personal) fields.addView(Button(this).apply { text="چسباندن از کلیپ‌بورد";setOnClickListener {
			val clip=(getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).primaryClip
			if(clip!=null && clip.itemCount>0) link?.setText(clip.getItemAt(0).coerceToText(this@ServerSettingsActivity))
		} })
		val scroll=ScrollView(this).apply { addView(fields) }
		val dialog=MaterialAlertDialogBuilder(this).setTitle(if(personal) "کانفیگ شخصی" else "سرور پروکسی")
			.setView(scroll).setNegativeButton("انصراف",null).setPositiveButton("ذخیره",null).create()
		dialog.setOnShowListener { dialog.getButton(-1).setOnClickListener {
			if(!canChange()) return@setOnClickListener
			val title=name.text.toString().trim()
			if(title.isBlank() || title.length>80) { name.error="نامی بین ۱ تا ۸۰ نویسه وارد کنید.";return@setOnClickListener }
			val profile=if(personal) {
				val value=link!!.text.toString().trim()
				try { PersonalConfig.outbound(value) } catch(_:Exception) {
					link.error="لینک نامعتبر یا تنظیمات پشتیبانی‌نشده است. لینک تکی VLESS، VMess یا Trojan وارد کنید؛ نه لینک اشتراک.";return@setOnClickListener
				}
				ServerProfile(old?.id ?: ServerProfiles.newId(),title,link=value)
			} else {
				val endpoint=ProxyEndpoint.parse(host!!.text.toString(),port!!.text.toString())
				if(endpoint==null) { host.error=getString(R.string.invalid_proxy_endpoint);return@setOnClickListener }
				ServerProfile(old?.id ?: ServerProfiles.newId(),title,endpoint.host,endpoint.port)
			}
			val all=ServerProfiles.load(this).toMutableList();val index=all.indexOfFirst { it.id==profile.id }
			if(index<0) all.add(profile) else all[index]=profile
			ServerProfiles.save(this,all);ServerProfiles.select(this,profile.id);dialog.dismiss();render()
		} };dialog.show()
	}
}
