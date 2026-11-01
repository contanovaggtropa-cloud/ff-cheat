package com.ff.cheat

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 72, 48, 72) }
        fun btn(label: String, action: () -> Unit) = Button(this).apply { text = label; setOnClickListener { action() } }
        fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        root.addView(TextView(this).apply { text = "FF Cheat — Setup"; textSize = 22f; setPadding(0,0,0,24) })
        root.addView(btn("1. Grant Overlay Permission") {
            startActivityForResult(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")), 10)
        })
        root.addView(btn("2. Enable Accessibility Service") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        root.addView(btn("3. START CHEAT") {
            if (!Settings.canDrawOverlays(this)) { toast("Grant overlay permission first"); return@btn }
            startForegroundService(Intent(this, OverlayService::class.java))
            toast("Cheat running — open FF")
        })
        root.addView(btn("4. STOP CHEAT") {
            stopService(Intent(this, OverlayService::class.java)); toast("Stopped")
        })
        root.addView(TextView(this).apply {
            text = "\n• Root obrigatório para mem read + no-recoil\n• Atualiza offsets em MemReader.kt após cada patch"
            textSize = 12f
        })
        setContentView(root)
    }
}
