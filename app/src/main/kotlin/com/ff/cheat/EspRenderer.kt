package com.ff.cheat

import android.graphics.*

object EspRenderer {
    private val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(255, 255, 48, 48); style = Paint.Style.STROKE
        strokeWidth = 4f; strokeCap = Paint.Cap.ROUND
    }
    private val snapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(180, 0, 220, 255); style = Paint.Style.STROKE; strokeWidth = 1.5f
    }
    private val hpBg   = Paint().apply { color = Color.argb(180, 0, 0, 0); style = Paint.Style.FILL }
    private val hpFill = Paint().apply { style = Paint.Style.FILL }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 26f; typeface = Typeface.MONOSPACE
        setShadowLayer(3f, 1f, 1f, Color.BLACK)
    }

    fun render(canvas: Canvas, players: List<PlayerData>, sw: Int, sh: Int, snaplines: Boolean) {
        for (p in players) {
            if (p.scrFeetX !in 0f..sw.toFloat() || p.scrFeetY !in 0f..sh.toFloat()) continue
            val boxH = (p.scrFeetY - p.scrHeadY).coerceAtLeast(24f)
            val boxW = boxH * 0.42f
            val l = p.scrFeetX - boxW / 2f; val r = p.scrFeetX + boxW / 2f
            val t = p.scrHeadY;             val b = p.scrFeetY
            drawCorners(canvas, l, t, r, b, boxW * 0.28f)
            val hpRatio = (p.hp / p.maxHp.coerceAtLeast(1f)).coerceIn(0f, 1f)
            val barX = l - 8f
            canvas.drawRect(barX - 5f, t, barX, b, hpBg)
            hpFill.color = hpColor(hpRatio)
            canvas.drawRect(barX - 5f, b - boxH * hpRatio, barX, b, hpFill)
            canvas.drawText("${p.dist.toInt()}m", l, t - 5f, textPaint)
            canvas.drawText("${p.hp.toInt()}/${p.maxHp.toInt()}", l, b + 22f, textPaint)
            if (snaplines) canvas.drawLine(sw / 2f, sh.toFloat(), p.scrFeetX, p.scrFeetY, snapPaint)
        }
    }

    private fun drawCorners(c: Canvas, l: Float, t: Float, r: Float, b: Float, len: Float) {
        c.drawLine(l, t, l+len, t, cornerPaint); c.drawLine(l, t, l, t+len, cornerPaint)
        c.drawLine(r, t, r-len, t, cornerPaint); c.drawLine(r, t, r, t+len, cornerPaint)
        c.drawLine(l, b, l+len, b, cornerPaint); c.drawLine(l, b, l, b-len, cornerPaint)
        c.drawLine(r, b, r-len, b, cornerPaint); c.drawLine(r, b, r, b-len, cornerPaint)
    }

    private fun hpColor(r: Float) = when {
        r > 0.6f -> Color.rgb(0, 220, 80)
        r > 0.3f -> Color.rgb(230, 200, 0)
        else     -> Color.rgb(255, 50, 50)
    }

    fun w2s(wx: Float, wy: Float, wz: Float, vp: FloatArray, sw: Int, sh: Int): Pair<Float, Float>? {
        val w = vp[3]*wx + vp[7]*wy + vp[11]*wz + vp[15]
        if (w < 0.05f) return null
        val x = vp[0]*wx + vp[4]*wy + vp[8]*wz + vp[12]
        val y = vp[1]*wx + vp[5]*wy + vp[9]*wz + vp[13]
        return Pair((x/w + 1f)/2f * sw, (1f - y/w)/2f * sh)
    }
}
