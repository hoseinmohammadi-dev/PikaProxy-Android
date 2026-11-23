package io.github.ulysseszh.sunproxy

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

object ServerSettingsView {
 fun create(context: Context, profiles: List<ServerProfile>, selected: String,
  onBack: () -> Unit, onSelect: (ServerProfile) -> Unit, onEdit: (ServerProfile) -> Unit,
  onDelete: (ServerProfile) -> Unit, onAdd: (Boolean) -> Unit): LinearLayout {
  fun dp(n:Int)=(n*context.resources.displayMetrics.density).toInt()
  val ink=Color.rgb(16,26,59);val blue=Color.rgb(6,75,255);val muted=Color.rgb(74,88,120)
  fun label(value:String,size:Float,color:Int=ink,bold:Boolean=false)=TextView(context).apply {
   text=value;textSize=size;setTextColor(color);gravity=Gravity.RIGHT
   if(bold) setTypeface(typeface,Typeface.BOLD)
  }
  fun icon(resource:Int,description:String,tint:Int,action:()->Unit)=ImageButton(context).apply {
   setImageResource(resource);contentDescription=description;imageTintList=ColorStateList.valueOf(tint)
   setBackgroundResource(if(tint==Color.rgb(237,55,55)) R.drawable.bg_icon_red else R.drawable.bg_icon_blue);setPadding(dp(12),dp(12),dp(12),dp(12));setOnClickListener { action() }
  }
  fun actionButton(text:String,resource:Int,outlined:Boolean,action:()->Unit)=MaterialButton(context).apply {
   this.text=text;textSize=17f;setTypeface(typeface,Typeface.BOLD);cornerRadius=dp(15)
   backgroundTintList=ColorStateList.valueOf(if(outlined)Color.WHITE else blue)
   setTextColor(if(outlined)ink else Color.WHITE);strokeWidth=if(outlined)dp(1) else 0
   strokeColor=ColorStateList.valueOf(blue);icon=ContextCompat.getDrawable(context,resource)
   iconTint=ColorStateList.valueOf(if(outlined)blue else Color.WHITE);iconGravity=MaterialButton.ICON_GRAVITY_TEXT_END
   layoutDirection=View.LAYOUT_DIRECTION_LTR;setOnClickListener { action() }
  }
  val list=LinearLayout(context).apply { orientation=LinearLayout.VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL }
  val header=FrameLayout(context).apply { layoutDirection=View.LAYOUT_DIRECTION_RTL }
  header.addView(label("مدیریت سرورها",25f,bold=true).apply { gravity=Gravity.CENTER },FrameLayout.LayoutParams(-1,dp(56)).apply { leftMargin=dp(46);rightMargin=dp(46) })
  header.addView(icon(R.drawable.ic_back,"بازگشت",ink,onBack),FrameLayout.LayoutParams(dp(44),dp(44),Gravity.RIGHT or Gravity.CENTER_VERTICAL))
  list.addView(header)
  list.addView(label("سرور موردنظر خود را انتخاب کنید",16f,muted).apply { gravity=Gravity.CENTER },LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(12);bottomMargin=dp(22) })
  for(profile in profiles) {
   val checked=profile.id==selected
   val card=LinearLayout(context).apply {
    orientation=LinearLayout.VERTICAL;layoutDirection=View.LAYOUT_DIRECTION_RTL;setPadding(dp(12),dp(14),dp(8),dp(12))
    setBackgroundResource(if(checked)R.drawable.bg_selected_server else R.drawable.bg_server_settings)
   }
   val row=LinearLayout(context).apply { orientation=LinearLayout.HORIZONTAL;layoutDirection=View.LAYOUT_DIRECTION_LTR;gravity=Gravity.CENTER_VERTICAL }
   val names=LinearLayout(context).apply { orientation=LinearLayout.VERTICAL;gravity=Gravity.RIGHT;setOnClickListener { onSelect(profile) } }
   names.addView(label(profile.name,17f,bold=true),LinearLayout.LayoutParams(-1,-2))
   names.addView(label(profile.summary,15f,muted).apply { textDirection=View.TEXT_DIRECTION_LTR },LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(6) })
   row.addView(icon(R.drawable.ic_delete,"حذف ${profile.name}",Color.rgb(237,55,55)) { onDelete(profile) },LinearLayout.LayoutParams(dp(44),dp(44)))
   row.addView(icon(R.drawable.ic_edit,"ویرایش ${profile.name}",blue) { onEdit(profile) },LinearLayout.LayoutParams(dp(44),dp(44)).apply { leftMargin=dp(8);rightMargin=dp(8) })
   row.addView(names,LinearLayout.LayoutParams(0,-2,1f))
   row.addView(RadioButton(context).apply {
    isChecked=checked;buttonTintList=ColorStateList.valueOf(if(checked)blue else Color.rgb(182,192,209))
    contentDescription="انتخاب ${profile.name}";setOnClickListener { onSelect(profile) }
   },LinearLayout.LayoutParams(dp(42),dp(48)))
   card.addView(row)
   if(checked) card.addView(label("انتخاب‌شده",12f).apply {
    gravity=Gravity.CENTER;setBackgroundResource(R.drawable.bg_info);setPadding(dp(12),dp(3),dp(12),dp(3))
   },LinearLayout.LayoutParams(-2,dp(28)).apply { gravity=Gravity.RIGHT;topMargin=dp(10);rightMargin=dp(42) })
   list.addView(card,LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(14) })
  }
  list.addView(actionButton("افزودن سرور",R.drawable.ic_plus,false) { onAdd(false) },LinearLayout.LayoutParams(-1,dp(60)).apply { topMargin=dp(28) })
  list.addView(actionButton("وارد کردن کانفیگ شخصی",R.drawable.ic_import_config,true) { onAdd(true) },LinearLayout.LayoutParams(-1,dp(58)).apply { topMargin=dp(8) })
  val chips=LinearLayout(context).apply { orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER;layoutDirection=View.LAYOUT_DIRECTION_LTR }
  for(name in listOf("Trojan","VLESS","VMess")) chips.addView(label(name,13f,muted).apply {
   gravity=Gravity.CENTER;setBackgroundResource(R.drawable.bg_chip)
  },LinearLayout.LayoutParams(dp(78),dp(32)).apply { marginStart=dp(7);marginEnd=dp(7) })
  list.addView(chips,LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(20) })
  return list
 }
}
