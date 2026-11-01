package com.ff.cheat

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.*
import android.view.*
import android.widget.*
import androidx.core.app.NotificationCompat

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private lateinit var esp: EspCanvas
    private lateinit var menu: MenuOverlay
    private val core = CheatCore()
    override fun onBind(i: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        startForeground(1, buildNotif())
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val dm = resources.displayMetrics
        core.screenW = dm.widthPixels; core.screenH = dm.heightPixels

        esp = EspCanvas(this, core)
        wm.addView(esp, WindowManager.LayoutParams(dm.widthPixels, dm.heightPixels,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.START })

        menu = MenuOverlay(this, core, onStop = ::stopSelf)
        val mlp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 20; y = 180 }
        wm.addView(menu, mlp)

        var ox=0; var oy=0; var rx=0f; var ry=0f
        menu.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { ox=mlp.x; oy=mlp.y; rx=e.rawX; ry=e.rawY }
                MotionEvent.ACTION_MOVE -> { mlp.x=ox+(e.rawX-rx).toInt(); mlp.y=oy+(e.rawY-ry).toInt(); wm.updateViewLayout(menu,mlp) }
            }; false
        }
        core.start()
        val h = Handler(Looper.getMainLooper())
        val tick = object : Runnable { override fun run() { esp.invalidate(); h.postDelayed(this, 8) } }
        h.post(tick)
    }

    override fun onDestroy() {
        core.stop()
        runCatching { wm.removeView(esp) }
        runCatching { wm.removeView(menu) }
        super.onDestroy()
    }

    private fun buildNotif(): Notification {
        val ch = NotificationChannel("ff", "Overlay", NotificationManager.IMPORTANCE_MIN)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        return NotificationCompat.Builder(this, "ff").setContentTitle("System Service")
            .setSmallIcon(android.R.drawable.ic_dialog_info).build()
    }
}

class EspCanvas(ctx: Context, private val core: CheatCore) : View(ctx) {
    init { setLayerType(LAYER_TYPE_HARDWARE, null) }
    override fun onDraw(canvas: Canvas) { core.drawEsp(canvas) }
}

class MenuOverlay(ctx: Context, private val core: CheatCore, private val onStop: () -> Unit) : LinearLayout(ctx) {
    init {
        orientation = VERTICAL
        setBackgroundColor(Color.argb(215, 15, 15, 15))
        setPadding(28, 28, 28, 28)
        addView(TextView(ctx).apply { text = "FF CHEAT"; textSize = 13f; setTextColor(Color.argb(200,200,200,200)); setPadding(0,0,0,12) })
        addToggle("ESP Boxes",  core.espEnabled)      { core.espEnabled = it }
        addToggle("Snaplines",  core.snapEnabled)     { core.snapEnabled = it }
        addToggle("Aimbot",     core.aimbotEnabled)   { core.aimbotEnabled = it }
        addToggle("No-Recoil",  core.noRecoilEnabled) { core.noRecoilEnabled = it }
        addView(Button(ctx).apply {
            text = "■ STOP"; setBackgroundColor(Color.argb(210,180,0,0)); setTextColor(Color.WHITE)
            setOnClickListener { onStop() }
        })
    }
    private fun addToggle(name: String, init: Boolean, cb: (Boolean) -> Unit) {
        addView(LinearLayout(context).apply {
            orientation = HORIZONTAL
            addView(TextView(context).apply { text = name; setTextColor(Color.WHITE); textSize = 15f
                layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f) })
            addView(Switch(context).apply { isChecked = init; setOnCheckedChangeListener { _, v -> cb(v) } })
        })
    }
}
